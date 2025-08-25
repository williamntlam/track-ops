package com.trackops.server.application.services.orders;

import org.springframework.stereotype.Service;
import com.trackops.server.ports.input.orders.OrderServicePort;
import com.trackops.server.ports.output.persistence.orders.OrderRepository;
import com.trackops.server.ports.output.events.orders.OrderEventProducer;
import com.trackops.server.adapters.input.web.dto.mappers.OrderMapper;
import com.trackops.server.adapters.input.web.dto.CreateOrderRequest;
import com.trackops.server.adapters.input.web.dto.UpdateOrderStatusRequest;
import com.trackops.server.adapters.input.web.dto.OrderResponse;
import com.trackops.server.adapters.input.web.dto.OrderListResponse;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.model.enums.OrderStatus;
import com.trackops.server.domain.events.orders.OrderCreatedEvent;
import com.trackops.server.domain.events.orders.OrderStatusUpdatedEvent;
import com.trackops.server.domain.events.orders.OrderCancelledEvent;
import com.trackops.server.domain.events.orders.OrderDeliveredEvent;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService implements OrderServicePort {
    
    private OrderRepository orderRepository;
    private OrderEventProducer orderEventProducer;
    private OrderMapper orderMapper;

    public OrderService() {

    }

    public OrderService(OrderRepository orderRepository, OrderEventProducer orderEventProducer, OrderMapper orderMapper) {

        this.orderRepository = orderRepository;
        this.orderEventProducer = orderEventProducer;
        this.orderMapper = orderMapper;

    }

    @Override 
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Step 1: Parse the data from CreateOrderRequest
        UUID customerId = request.getCustomerId();
        BigDecimal totalAmount = request.getTotalAmount();
        AddressDTO addressDTO = request.getAddress();
        String deliveryInstructions = request.getDeliveryInstructions();

        // Step 2: Convert AddressDTO to Address domain object
        Address address = addressDTOToAddress(addressDTO);

        // Step 3: Create new Order (let lifecycle hooks handle timestamps)
        Order newOrder = new Order(customerId, OrderStatus.PENDING, totalAmount, address, deliveryInstructions);

        // Step 4: Save to database and capture the saved order
        Order savedOrder = orderRepository.save(newOrder);

        // Step 5: Publish event
        OrderCreatedEvent event = new OrderCreatedEvent(savedOrder.getId(), "system");
        orderEventProducer.publishOrderCreated(event);

        // Step 6: Return mapped response
        return orderMapper.orderToOrderResponse(savedOrder);
    }

    @Override
    public OrderResponse getOrderById(UUID orderId) {

        return OrderRepository.findById(orderId);

    }

    @Override
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        // Step 1: Find the existing order
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Step 2: Update the order status based on the new status
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
        
        // Step 3: Save the updated order
        Order updatedOrder = orderRepository.save(order);
        
        // Step 4: Publish event
        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
            orderId, 
            order.getStatus(), 
            newStatus, 
            updatedOrder.getVersion()
        );
        orderEventProducer.publishOrderStatusUpdated(event);
        
        // Step 5: Return updated response
        return orderMapper.orderToOrderResponse(updatedOrder);
    }

    @Override
    public OrderResponse cancelOrder(UUID orderId) {

    }

    @Override
    public List<OrderResponse> getAllOrders() {

    }

    @Override
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {

    }

    @Override
    public List<OrderResponse> getOrdersByCustomerId(UUID customerId) {

    }

    @Override
    public OrderResponse confirmOrder(UUID orderId) {

    }

    @Override
    public OrderResponse processOrder(UUID orderId) {

    }

    @Override
    public OrderResponse shipOrder(UUID orderId) {

    }

    @Override
    public OrderResponse deliverOrder(UUID orderId) {

    }

}