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

        // Step 1: Parse the data in the CreateOrderRequest
        // object so that we can feed it into the OrderRepository.

        UUID customerId = request.getCustomerId();
        BigDecimal totalAmount = request.getTotalAmount();
        AddressDTO address = request.getAddress();
        String deliveryInstructions = request.getDeliveryInstructions();

        OrderRepository.createOrder();

        // Step 2: Use OrderRepository to save the created order.

        // Step 3: Create a OrderResponse object, fill it accordingly
        // and return this OrderResponse object.

    }

    @Override
    public OrderResponse getOrderById(UUID orderId) {

    }

    @Override
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {

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