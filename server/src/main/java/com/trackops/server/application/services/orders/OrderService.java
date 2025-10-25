package com.trackops.server.application.services.orders;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.trackops.server.ports.input.orders.OrderServicePort;
import com.trackops.server.ports.output.persistence.orders.OrderRepository;
import com.trackops.server.ports.output.events.orders.OrderEventProducer;
import com.trackops.server.adapters.input.web.dto.mappers.OrderMapper;
import com.trackops.server.adapters.input.web.dto.CreateOrderRequest;
import com.trackops.server.adapters.input.web.dto.UpdateOrderStatusRequest;
import com.trackops.server.adapters.input.web.dto.OrderResponse;
import com.trackops.server.adapters.input.web.dto.OrderListResponse;
import com.trackops.server.adapters.input.web.dto.AddressDTO;
import com.trackops.server.domain.model.OperationResult;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.model.orders.Address;
import com.trackops.server.domain.model.enums.OrderStatus;
import com.trackops.server.domain.events.orders.OrderCreatedEvent;
import com.trackops.server.domain.events.orders.OrderStatusUpdatedEvent;
import com.trackops.server.domain.events.orders.OrderCancelledEvent;
import com.trackops.server.domain.events.orders.OrderDeliveredEvent;
import com.trackops.server.domain.exceptions.OrderNotFoundException;
import com.trackops.server.domain.exceptions.OrderValidationException;
import com.trackops.server.domain.exceptions.InvalidOrderStatusTransitionException;
import com.trackops.server.application.services.saga.SagaOrchestratorService;
import com.trackops.server.application.services.outbox.OutboxEventService;
import com.trackops.server.ports.output.cache.OrderStatusCachePort;
import com.trackops.server.ports.output.cache.OrderCachePort;
import java.time.Duration;
import java.util.Optional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderService implements OrderServicePort {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;
    private final OrderMapper orderMapper;
    private final SagaOrchestratorService sagaOrchestratorService;
    private final OutboxEventService outboxEventService;
    private final OrderStatusCachePort orderStatusCachePort;
    private final OrderCachePort orderCachePort;

    public OrderService(OrderRepository orderRepository, OrderEventProducer orderEventProducer, 
                       OrderMapper orderMapper, SagaOrchestratorService sagaOrchestratorService,
                       OutboxEventService outboxEventService, OrderStatusCachePort orderStatusCachePort,
                       OrderCachePort orderCachePort) {
        this.orderRepository = orderRepository;
        this.orderEventProducer = orderEventProducer;
        this.orderMapper = orderMapper;
        this.sagaOrchestratorService = sagaOrchestratorService;
        this.outboxEventService = outboxEventService;
        this.orderStatusCachePort = orderStatusCachePort;
        this.orderCachePort = orderCachePort;
    }

    @Override 
    public OrderResponse createOrder(CreateOrderRequest request) {
        try {
            // Step 1: Parse the data from CreateOrderRequest
            UUID customerId = request.getCustomerId();
            BigDecimal totalAmount = request.getTotalAmount();
            AddressDTO addressDTO = request.getAddress();
            String deliveryInstructions = request.getDeliveryInstructions();

            // Step 2: Validate input data
            if (customerId == null) {
                throw new OrderValidationException("Customer ID cannot be null");
            }
            if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new OrderValidationException("Total amount must be greater than zero");
            }
            if (addressDTO == null) {
                throw new OrderValidationException("Address cannot be null");
            }

            // Step 3: Convert AddressDTO to Address domain object
            Address address = orderMapper.addressDTOToAddress(addressDTO);
            if (address == null) {
                throw new OrderValidationException("Failed to convert address");
            }

            // Step 4: Create new Order (let lifecycle hooks handle timestamps)
            Order newOrder = new Order(customerId, OrderStatus.PENDING, totalAmount, address, deliveryInstructions);

            // Step 5: Save to database and capture the saved order
            Order savedOrder = orderRepository.save(newOrder);
            if (savedOrder == null) {
                throw new RuntimeException("Failed to save order to database");
            }

            // Step 6: Create outbox event (within same transaction)
            try {
                OrderCreatedEvent event = new OrderCreatedEvent(savedOrder.getId(), "system");
                outboxEventService.createOrderEvent(
                    savedOrder.getId().toString(),
                    "ORDER_CREATED",
                    event
                );
            } catch (Exception e) {
                // Log the outbox event creation failure but don't fail the order creation
                // The outbox event will be retried by the publisher
            }

            // Step 7: Cache the new order
            try {
                orderCachePort.cacheOrder(savedOrder, Duration.ofHours(1));
                logger.debug("Cached new order: {}", savedOrder.getId());
            } catch (Exception e) {
                logger.warn("Failed to cache new order {}: {}", savedOrder.getId(), e.getMessage());
            }

            // Step 8: Return mapped response
            OrderResponse response = orderMapper.orderToOrderResponse(savedOrder);
            if (response == null) {
                throw new RuntimeException("Failed to map order to response");
            }
            
            // Step 9: Cache the response
            try {
                orderCachePort.cacheOrderResponse(savedOrder.getId(), response, Duration.ofHours(1));
            } catch (Exception e) {
                logger.warn("Failed to cache new order response {}: {}", savedOrder.getId(), e.getMessage());
            }
            
            return response;

        } catch (OrderValidationException e) {
            // Re-throw validation exceptions as-is
            throw e;
        } catch (Exception e) {
            // Wrap unexpected exceptions in a service exception
            throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
        }
    }

    @Override
    public OrderResponse getOrderById(UUID orderId) {
        try {
            // Step 1: Validate input
            if (orderId == null) {
                throw new OrderValidationException("Order ID cannot be null");
            }
            
            // Step 2: Check cache first for OrderResponse
            Optional<OrderResponse> cachedResponse = orderCachePort.getOrderResponse(orderId);
            if (cachedResponse.isPresent()) {
                logger.debug("Cache hit for order response: {}", orderId);
                return cachedResponse.get();
            }

            // Step 3: Check cache for Order entity
            Optional<Order> cachedOrder = orderCachePort.getOrder(orderId);
            if (cachedOrder.isPresent()) {
                logger.debug("Cache hit for order entity: {}", orderId);
                OrderResponse response = orderMapper.orderToOrderResponse(cachedOrder.get());
                if (response == null) {
                    throw new RuntimeException("Failed to map cached order to response");
                }
                
                // Cache the response for future requests
                try {
                    orderCachePort.cacheOrderResponse(orderId, response, Duration.ofHours(1));
                } catch (Exception e) {
                    logger.warn("Failed to cache order response for {}: {}", orderId, e.getMessage());
                }
                
                return response;
            }

            // Step 4: Database lookup
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
            
            // Step 5: Map to response
            OrderResponse response = orderMapper.orderToOrderResponse(order);
            if (response == null) {
                throw new RuntimeException("Failed to map order to response");
            }
            
            // Step 6: Cache both entity and response
            try {
                orderCachePort.cacheOrder(order, Duration.ofHours(1));
                orderCachePort.cacheOrderResponse(orderId, response, Duration.ofHours(1));
                logger.debug("Cached order and response for: {}", orderId);
            } catch (Exception e) {
                logger.warn("Failed to cache order data for {}: {}", orderId, e.getMessage());
            }
            
            return response;
            
        } catch (OrderNotFoundException | OrderValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve order: " + e.getMessage(), e);
        }
    }

    @Override
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        try {
            // Step 1: Validate input parameters
            if (orderId == null) {
                throw new OrderValidationException("Order ID cannot be null");
            }
            if (newStatus == null) {
                throw new OrderValidationException("New status cannot be null");
            }

            // Step 2: Find the existing order
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
            
            // Step 3: Validate current order state
            if (order.getStatus() == OrderStatus.DELIVERED && newStatus == OrderStatus.CANCELLED) {
                throw new InvalidOrderStatusTransitionException("Delivered orders cannot be cancelled");
            }
            if (order.getStatus() == OrderStatus.CANCELLED) {
                throw new InvalidOrderStatusTransitionException("Cancelled orders cannot be updated");
            }

            // Step 4: Update the order status based on the new status
            OrderStatus previousStatus = order.getStatus();
            try {
                switch (newStatus) {
                    case CONFIRMED:
                        order.confirm();
                        break;
                    case PROCESSING:
                        order.process();
                        break;
                    case SHIPPED:
                        order.ship();
                        break;
                    case DELIVERED:
                        order.deliver();
                        break;
                    case CANCELLED:
                        order.cancel();
                        break;
                    default:
                        throw new InvalidOrderStatusTransitionException("Invalid status: " + newStatus);
                }
            } catch (IllegalStateException e) {
                // Business rule violation from Order entity
                throw new InvalidOrderStatusTransitionException("Invalid status transition: " + e.getMessage());
            }
            
            // Step 5: Save the updated order
            Order updatedOrder = orderRepository.save(order);
            if (updatedOrder == null) {
                throw new RuntimeException("Failed to save updated order to database");
            }
            
            // Step 5.5: Invalidate and update caches
            try {
                // Invalidate existing caches
                orderCachePort.invalidateAllOrderCaches(orderId);
                
                // Update status cache (legacy)
                orderStatusCachePort.updateOrderStatus(orderId, newStatus, Duration.ofHours(1));
                
                // Cache the updated order and response
                orderCachePort.cacheOrder(updatedOrder, Duration.ofHours(1));
                OrderResponse updatedResponse = orderMapper.orderToOrderResponse(updatedOrder);
                if (updatedResponse != null) {
                    orderCachePort.cacheOrderResponse(orderId, updatedResponse, Duration.ofHours(1));
                }
                
                logger.debug("Updated caches for order status {} -> {}", orderId, newStatus);

            } catch (Exception e) {
                logger.warn("Failed to update cache for order {}: {}", orderId, e.getMessage());
            }

            // Step 6: Create outbox event (within same transaction)
            try {
                OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
                    orderId, 
                    previousStatus,  // Use the status before update
                    newStatus, 
                    updatedOrder.getVersion()
                );
                outboxEventService.createOrderEvent(
                    orderId.toString(),
                    "ORDER_STATUS_UPDATED",
                    event
                );
            } catch (Exception e) {
                // Log the outbox event creation failure but don't fail the status update
                // The outbox event will be retried by the publisher
            }
            
            // Step 7: Return updated response
            OrderResponse response = orderMapper.orderToOrderResponse(updatedOrder);
            if (response == null) {
                throw new RuntimeException("Failed to map updated order to response");
            }
            
            return response;

        } catch (OrderNotFoundException | OrderValidationException | InvalidOrderStatusTransitionException e) {
            // Re-throw business exceptions as-is
            throw e;
        } catch (Exception e) {
            // Wrap unexpected exceptions in a service exception
            throw new RuntimeException("Failed to update order status: " + e.getMessage(), e);
        }
    }

    @Override
    public OrderResponse cancelOrder(UUID orderId) {
        try {
            // Step 1: Validate input
            if (orderId == null) {
                throw new OrderValidationException("Order ID cannot be null");
            }

            // Step 2: Find the existing order
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

            // Step 3: Validate order can be cancelled
            if (order.getStatus() == OrderStatus.DELIVERED) {
                throw new OrderValidationException("Delivered orders cannot be cancelled");
            }

            try {

                orderStatusCachePort.updateOrderStatus(orderId, OrderStatus.CANCELLED, Duration.ofHours(1));
                logger.debug("Updated order {} to have a status of cancelled", orderId);

            } catch (Exception e) {

                logger.debug("Error occurred in updating the cache for order {} to have a status of cancelled: {}", orderId, e.getMessage());

            }

            // Step 4: Start SAGA for order cancellation
            UUID sagaId = sagaOrchestratorService.startOrderCancellationSaga(orderId);

            // Step 5: Return current order response (SAGA will handle the rest asynchronously)
            OrderResponse response = orderMapper.orderToOrderResponse(order);
            if (response == null) {
                throw new RuntimeException("Failed to map order to response");
            }

            return response;

        } catch (OrderNotFoundException | OrderValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to cancel order: " + e.getMessage(), e);
        }
    }

    @Override
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        try {
            // Step 1: Generate cache key for this page
            String pageKey = String.format("page_%d_size_%d_sort_%s", 
                pageable.getPageNumber(), 
                pageable.getPageSize(), 
                pageable.getSort().toString());
            
            // Step 2: Check cache first
            Optional<Page<OrderResponse>> cachedPage = orderCachePort.getOrderPage(pageKey);
            if (cachedPage.isPresent()) {
                logger.debug("Cache hit for orders page: {}", pageKey);
                return cachedPage.get();
            }
            
            // Step 3: Get paginated orders from repository
            Page<Order> orderPage = orderRepository.findAll(pageable);
            if (orderPage == null) {
                throw new RuntimeException("Repository returned null orders page");
            }
            
            // Step 4: Convert each Order to OrderResponse
            Page<OrderResponse> responsePage = orderPage.map(order -> {
                try {
                    return orderMapper.orderToOrderResponse(order);
                } catch (Exception e) {
                    logger.warn("Failed to map order {}: {}", order.getId(), e.getMessage());
                    return null;
                }
            });
            
            // Step 5: Cache the page (with shorter TTL for pagination)
            try {
                orderCachePort.cacheOrderPage(pageKey, responsePage, Duration.ofMinutes(15));
                logger.debug("Cached orders page: {}", pageKey);
            } catch (Exception e) {
                logger.warn("Failed to cache orders page {}: {}", pageKey, e.getMessage());
            }
            
            // Step 6: Return the paginated responses
            return responsePage;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve paginated orders: " + e.getMessage(), e);
        }
    }

    @Override
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        try {
            // Step 1: Validate input
            if (status == null) {
                throw new OrderValidationException("Status cannot be null");
            }
            
            // Step 2: Check cache first
            Optional<List<OrderResponse>> cachedOrders = orderCachePort.getOrdersByStatus(status.name());
            if (cachedOrders.isPresent()) {
                logger.debug("Cache hit for orders by status: {}", status);
                return cachedOrders.get();
            }
            
            // Step 3: Get orders by status from repository
            List<Order> orders = orderRepository.findByStatus(status);
            if (orders == null) {
                throw new RuntimeException("Repository returned null orders list");
            }
            
            // Step 4: Convert to responses
            List<OrderResponse> responses = orders.stream()
                .filter(Objects::nonNull)
                .map(orderMapper::orderToOrderResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            // Step 5: Cache the results
            try {
                orderCachePort.cacheOrdersByStatus(status.name(), responses, Duration.ofMinutes(30));
                logger.debug("Cached {} orders by status: {}", responses.size(), status);
            } catch (Exception e) {
                logger.warn("Failed to cache orders by status {}: {}", status, e.getMessage());
            }
            
            return responses;
            
        } catch (OrderValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve orders by status: " + e.getMessage(), e);
        }
    }

    @Override
    public List<OrderResponse> getOrdersByCustomerId(UUID customerId) {
        try {
            // Step 1: Validate input
            if (customerId == null) {
                throw new OrderValidationException("Customer ID cannot be null");
            }
            
            // Step 2: Check cache first
            Optional<List<OrderResponse>> cachedOrders = orderCachePort.getOrdersByCustomer(customerId);
            if (cachedOrders.isPresent()) {
                logger.debug("Cache hit for orders by customer: {}", customerId);
                return cachedOrders.get();
            }
            
            // Step 3: Get orders by customer ID from repository
            List<Order> orders = orderRepository.findByCustomerId(customerId);
            if (orders == null) {
                throw new RuntimeException("Repository returned null orders list");
            }
            
            // Step 4: Convert to responses
            List<OrderResponse> responses = orders.stream()
                .filter(Objects::nonNull)
                .map(orderMapper::orderToOrderResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            // Step 5: Cache the results
            try {
                orderCachePort.cacheOrdersByCustomer(customerId, responses, Duration.ofMinutes(30));
                logger.debug("Cached {} orders by customer: {}", responses.size(), customerId);
            } catch (Exception e) {
                logger.warn("Failed to cache orders by customer {}: {}", customerId, e.getMessage());
            }
            
            return responses;
            
        } catch (OrderValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve orders by customer ID: " + e.getMessage(), e);
        }
    }

    @Override
    public OrderResponse confirmOrder(UUID orderId) {
        try {
            // Step 1: Validate input
            if (orderId == null) {
                throw new OrderValidationException("Order ID cannot be null");
            }
            
            // Step 2: Find the existing order
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
            
            // Step 3: Validate order can be confirmed
            if (order.getStatus() != OrderStatus.PENDING) {
                throw new OrderValidationException("Only PENDING orders can be confirmed");
            }
            
            // Step 4: Start SAGA for order processing
            UUID sagaId = sagaOrchestratorService.startOrderProcessingSaga(orderId);
            
            // Step 5: Return current order response (SAGA will handle the rest asynchronously)
            OrderResponse response = orderMapper.orderToOrderResponse(order);
            if (response == null) {
                throw new RuntimeException("Failed to map order to response");
            }
            
            return response;
            
        } catch (OrderNotFoundException | OrderValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to confirm order: " + e.getMessage(), e);
        }
    }

    @Override
    public OrderResponse processOrder(UUID orderId) {
        try {
            // Step 1: Validate input
            if (orderId == null) {
                throw new OrderValidationException("Order ID cannot be null");
            }
            
            // Step 2: Find the existing order
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
            
            // Step 3: Use business method
            OrderStatus previousStatus = order.getStatus();
            order.process(); // Changes status to PROCESSING
            
            // Step 4: Save the updated order
            Order updatedOrder = orderRepository.save(order);
            if (updatedOrder == null) {
                throw new RuntimeException("Failed to save processed order to database");
            }
            
            // Step 5: Create outbox event (within same transaction)
            try {
                OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
                    orderId, 
                    previousStatus, 
                    OrderStatus.PROCESSING, 
                    updatedOrder.getVersion()
                );
                outboxEventService.createOrderEvent(
                    orderId.toString(),
                    "ORDER_STATUS_UPDATED",
                    event
                );
            } catch (Exception e) {
                // Log error but don't fail the processing
                // The outbox event will be retried by the publisher
                logger.error("Error occurred trying to process order {}: {}", orderId, e.getMessage());
            }
            
            // Step 6: Return updated response
            OrderResponse response = orderMapper.orderToOrderResponse(updatedOrder);
            if (response == null) {
                throw new RuntimeException("Failed to map processed order to response");
            }
            
            return response;
            
        } catch (OrderNotFoundException | OrderValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process order: " + e.getMessage(), e);
        }
    }

    @Override
    public OrderResponse shipOrder(UUID orderId) {
        try {
            // Step 1: Validate input
            if (orderId == null) {
                throw new OrderValidationException("Order ID cannot be null");
            }
            
            // Step 2: Find the existing order
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
            
            // Step 3: Use business method
            OrderStatus previousStatus = order.getStatus();
            order.ship(); // Changes status to SHIPPED
            
            // Step 4: Save the updated order
            Order updatedOrder = orderRepository.save(order);
            if (updatedOrder == null) {
                throw new RuntimeException("Failed to save shipped order to database");
            }
            
            // Step 5: Create outbox event (within same transaction)
            try {
                OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
                    orderId, 
                    previousStatus, 
                    OrderStatus.SHIPPED, 
                    updatedOrder.getVersion()
                );
                outboxEventService.createOrderEvent(
                    orderId.toString(),
                    "ORDER_STATUS_UPDATED",
                    event
                );
            } catch (Exception e) {
                // Log error but don't fail the shipping
                // The outbox event will be retried by the publisher
            }
            
            // Step 6: Return updated response
            OrderResponse response = orderMapper.orderToOrderResponse(updatedOrder);
            if (response == null) {
                throw new RuntimeException("Failed to map shipped order to response");
            }
            
            return response;
            
        } catch (OrderNotFoundException | OrderValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to ship order: " + e.getMessage(), e);
        }
    }

    @Override
    public OrderResponse deliverOrder(UUID orderId) {
        try {
            // Step 1: Validate input
            if (orderId == null) {
                throw new OrderValidationException("Order ID cannot be null");
            }
            
            // Step 2: Find the existing order
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
            
            // Step 3: Use business method
            OrderStatus previousStatus = order.getStatus();
            order.deliver(); // Changes status to DELIVERED
            
            // Step 4: Save the updated order
            Order updatedOrder = orderRepository.save(order);
            if (updatedOrder == null) {
                throw new RuntimeException("Failed to save delivered order to database");
            }
            
            // Step 5: Create outbox event (within same transaction)
            try {
                OrderDeliveredEvent event = new OrderDeliveredEvent(
                    orderId, 
                    LocalDateTime.now() // or get from request
                );
                outboxEventService.createOrderEvent(
                    orderId.toString(),
                    "ORDER_DELIVERED",
                    event
                );
            } catch (Exception e) {
                // Log error but don't fail the delivery
                // The outbox event will be retried by the publisher
            }
            
            // Step 6: Return updated response
            OrderResponse response = orderMapper.orderToOrderResponse(updatedOrder);
            if (response == null) {
                throw new RuntimeException("Failed to map delivered order to response");
            }
            
            return response;
            
        } catch (OrderNotFoundException | OrderValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deliver order: " + e.getMessage(), e);
        }
    }
}