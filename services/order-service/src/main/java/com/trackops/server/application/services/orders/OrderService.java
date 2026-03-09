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
import com.trackops.server.application.services.events.EventPublishingService;
import com.trackops.server.ports.output.cache.OrderStatusCachePort;
import com.trackops.server.ports.output.cache.OrderCachePort;
import com.trackops.server.ports.output.cache.DistributedLockPort;
import com.trackops.server.adapters.output.monitoring.MetricsService;
import com.trackops.server.adapters.output.logging.StructuredLoggingService;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Optional;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderService implements OrderServicePort {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;
    private final OrderMapper orderMapper;
    private final SagaOrchestratorService sagaOrchestratorService;
    private final EventPublishingService eventPublishingService;
    private final OrderStatusCachePort orderStatusCachePort;
    private final OrderCachePort orderCachePort;
    private final DistributedLockPort distributedLockPort;
    private final OrderCacheBackgroundRefresher backgroundRefresher;
    private final MetricsService metricsService;
    private final StructuredLoggingService loggingService;

    @Value("${app.cache.load-lock.wait-seconds:10}")
    private long loadLockWaitSeconds;
    @Value("${app.cache.load-lock.lease-seconds:30}")
    private long loadLockLeaseSeconds;
    @Value("${app.cache.probabilistic-refresh.enabled:true}")
    private boolean probabilisticRefreshEnabled;
    @Value("${app.cache.probabilistic-refresh.window-ratio:0.2}")
    private double probabilisticRefreshWindowRatio;
    @Value("${app.cache.ttl.order:3600}")
    private long expectedOrderTtlSeconds;

    public OrderService(OrderRepository orderRepository, OrderEventProducer orderEventProducer,
                        OrderMapper orderMapper, SagaOrchestratorService sagaOrchestratorService,
                        EventPublishingService eventPublishingService, OrderStatusCachePort orderStatusCachePort,
                        OrderCachePort orderCachePort, DistributedLockPort distributedLockPort,
                        OrderCacheBackgroundRefresher backgroundRefresher,
                        MetricsService metricsService, StructuredLoggingService loggingService) {
        this.orderRepository = orderRepository;
        this.orderEventProducer = orderEventProducer;
        this.orderMapper = orderMapper;
        this.sagaOrchestratorService = sagaOrchestratorService;
        this.eventPublishingService = eventPublishingService;
        this.orderStatusCachePort = orderStatusCachePort;
        this.orderCachePort = orderCachePort;
        this.distributedLockPort = distributedLockPort;
        this.backgroundRefresher = backgroundRefresher;
        this.metricsService = metricsService;
        this.loggingService = loggingService;
    }

    @Override 
    public OrderResponse createOrder(CreateOrderRequest request) {
        Timer.Sample sample = metricsService.startOrderProcessingTimer();
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

            // Step 6: Publish order created event using the configured strategy (Outbox or Debezium)
            try {
                eventPublishingService.publishOrderCreated(savedOrder);
            } catch (Exception e) {
                // Log the event publishing failure but don't fail the order creation
                logger.warn("Failed to publish order created event: {}", e.getMessage());
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
            
            // Step 10: Record metrics and logging
            metricsService.recordOrderCreated();
            metricsService.recordOrderProcessingTime(sample);
            long revenueInCents = totalAmount.multiply(new BigDecimal("100")).longValue();
            metricsService.updateTotalRevenue(metricsService.getTotalRevenue() + revenueInCents);
            
            // Log business event
            loggingService.logOrderEvent("ORDER_CREATED", savedOrder.getId().toString(), 
                customerId.toString(), Map.of(
                    "totalAmount", totalAmount,
                    "status", savedOrder.getStatus().toString(),
                    "deliveryInstructions", deliveryInstructions != null ? deliveryInstructions : "none"
                ));
            
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
            if (orderId == null) {
                throw new OrderValidationException("Order ID cannot be null");
            }

            // Step 1: Check cache first (response then entity)
            Optional<OrderResponse> cachedResponse = orderCachePort.getOrderResponse(orderId);
            if (cachedResponse.isPresent()) {
                logger.debug("Cache hit for order response: {}", orderId);
                triggerProbabilisticRefresh(orderId, orderCachePort.getOrderResponseRemainingTtl(orderId));
                return cachedResponse.get();
            }
            Optional<Order> cachedOrder = orderCachePort.getOrder(orderId);
            if (cachedOrder.isPresent()) {
                logger.debug("Cache hit for order entity: {}", orderId);
                OrderResponse response = orderMapper.orderToOrderResponse(cachedOrder.get());
                if (response == null) {
                    throw new RuntimeException("Failed to map cached order to response");
                }
                try {
                    orderCachePort.cacheOrderResponse(orderId, response, Duration.ofHours(1));
                } catch (Exception e) {
                    logger.warn("Failed to cache order response for {}: {}", orderId, e.getMessage());
                }
                triggerProbabilisticRefresh(orderId, orderCachePort.getOrderRemainingTtl(orderId));
                return response;
            }

            // Step 2: Cache miss – use distributed lock to prevent stampeding (single loader, others wait/retry)
            Duration waitTimeout = Duration.ofSeconds(loadLockWaitSeconds);
            Duration leaseTime = Duration.ofSeconds(loadLockLeaseSeconds);
            boolean lockAcquired = distributedLockPort.tryLockForOrderLoad(orderId, waitTimeout, leaseTime);
            try {
                if (lockAcquired) {
                    // Double-check cache (another replica might have filled it)
                    Optional<OrderResponse> recheck = orderCachePort.getOrderResponse(orderId);
                    if (recheck.isPresent()) {
                        triggerProbabilisticRefresh(orderId, orderCachePort.getOrderResponseRemainingTtl(orderId));
                        return recheck.get();
                    }
                    Optional<Order> recheckOrder = orderCachePort.getOrder(orderId);
                    if (recheckOrder.isPresent()) {
                        OrderResponse response = orderMapper.orderToOrderResponse(recheckOrder.get());
                        if (response != null) {
                            orderCachePort.cacheOrderResponse(orderId, response, Duration.ofHours(1));
                            triggerProbabilisticRefresh(orderId, orderCachePort.getOrderRemainingTtl(orderId));
                            return response;
                        }
                    }
                    return loadOrderFromDbAndCache(orderId);
                }
                // Did not acquire lock – wait and re-check cache (another thread is loading)
                long deadline = System.currentTimeMillis() + waitTimeout.toMillis();
                while (System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    Optional<OrderResponse> retryResponse = orderCachePort.getOrderResponse(orderId);
                    if (retryResponse.isPresent()) {
                        logger.debug("Cache hit after waiting for loader: {}", orderId);
                        triggerProbabilisticRefresh(orderId, orderCachePort.getOrderResponseRemainingTtl(orderId));
                        return retryResponse.get();
                    }
                    Optional<Order> retryOrder = orderCachePort.getOrder(orderId);
                    if (retryOrder.isPresent()) {
                        OrderResponse response = orderMapper.orderToOrderResponse(retryOrder.get());
                        if (response != null) {
                            orderCachePort.cacheOrderResponse(orderId, response, Duration.ofHours(1));
                            triggerProbabilisticRefresh(orderId, orderCachePort.getOrderRemainingTtl(orderId));
                            return response;
                        }
                    }
                }
                // Timeout waiting for loader – fallback to DB to avoid indefinite wait
                logger.debug("Lock wait timeout, loading order from DB: {}", orderId);
                return loadOrderFromDbAndCache(orderId);
            } finally {
                if (lockAcquired) {
                    distributedLockPort.unlockForOrderLoad(orderId);
                }
            }
        } catch (OrderNotFoundException | OrderValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve order: " + e.getMessage(), e);
        }
    }

    /**
     * Probabilistic early revalidation: on cache hit, with probability that increases as TTL runs down,
     * trigger a background refresh so the cache is refreshed before expiry and load is spread over time.
     */
    private void triggerProbabilisticRefresh(UUID orderId, Optional<Duration> remainingTtl) {
        if (!probabilisticRefreshEnabled || remainingTtl.isEmpty()) return;
        long remainingSeconds = remainingTtl.get().toSeconds();
        long windowSeconds = (long) (probabilisticRefreshWindowRatio * expectedOrderTtlSeconds);
        if (windowSeconds <= 0 || remainingSeconds >= windowSeconds) return;
        // Probability increases from 0 (at start of window) to 1 (at expiry): P = 1 - (remaining / window)
        double probability = 1.0 - (remainingSeconds / (double) windowSeconds);
        if (ThreadLocalRandom.current().nextDouble() < probability) {
            backgroundRefresher.refreshOrderAsync(orderId);
        }
    }

    /**
     * Load order from database and populate cache. Caller must hold the load lock when calling.
     */
    private OrderResponse loadOrderFromDbAndCache(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        OrderResponse response = orderMapper.orderToOrderResponse(order);
        if (response == null) {
            throw new RuntimeException("Failed to map order to response");
        }
        try {
            orderCachePort.cacheOrder(order, Duration.ofHours(1));
            orderCachePort.cacheOrderResponse(orderId, response, Duration.ofHours(1));
            logger.debug("Cached order and response for: {}", orderId);
        } catch (Exception e) {
            logger.warn("Failed to cache order data for {}: {}", orderId, e.getMessage());
        }
        return response;
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

            // Step 6: Publish order status updated event using the configured strategy
            try {
                eventPublishingService.publishOrderStatusUpdated(updatedOrder, previousStatus.toString());
            } catch (Exception e) {
                // Log the event publishing failure but don't fail the status update
                logger.warn("Failed to publish order status updated event: {}", e.getMessage());
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
            
            // Step 5: Publish order status updated event using the configured strategy
            try {
                eventPublishingService.publishOrderStatusUpdated(updatedOrder, previousStatus.toString());
            } catch (Exception e) {
                // Log error but don't fail the processing
                logger.warn("Failed to publish order processing event: {}", e.getMessage());
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
            
            // Step 5: Publish order status updated event using the configured strategy
            try {
                eventPublishingService.publishOrderStatusUpdated(updatedOrder, previousStatus.toString());
            } catch (Exception e) {
                // Log error but don't fail the shipping
                logger.warn("Failed to publish order shipping event: {}", e.getMessage());
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
            
            // Step 5: Publish order status updated event using the configured strategy
            try {
                eventPublishingService.publishOrderStatusUpdated(updatedOrder, previousStatus.toString());
            } catch (Exception e) {
                // Log error but don't fail the delivery
                logger.warn("Failed to publish order delivered event: {}", e.getMessage());
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