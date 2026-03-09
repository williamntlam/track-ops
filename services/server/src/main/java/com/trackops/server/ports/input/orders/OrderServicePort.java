package com.trackops.server.ports.input.orders;

import com.trackops.server.adapters.input.web.dto.CreateOrderRequest;
import com.trackops.server.adapters.input.web.dto.OrderResponse;
import com.trackops.server.domain.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.util.List;

public interface OrderServicePort {

    // CRUD operations
    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse getOrderById(UUID orderId);
    OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus);
    OrderResponse cancelOrder(UUID orderId);
        
    // Query operations
    Page<OrderResponse> getAllOrders(Pageable pageable);
    List<OrderResponse> getOrdersByStatus(OrderStatus status);
    List<OrderResponse> getOrdersByCustomerId(UUID customerId);
        
    // Business operations
    OrderResponse confirmOrder(UUID orderId);
    OrderResponse processOrder(UUID orderId);
    OrderResponse shipOrder(UUID orderId);
    OrderResponse deliverOrder(UUID orderId);

}