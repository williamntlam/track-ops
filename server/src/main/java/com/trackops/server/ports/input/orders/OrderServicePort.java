package com.trackops.server.ports.input.orders;

import com.trackops.server.adapters.input.web.dto.CreateOrderRequest;
import com.trackops.server.adapters.input.web.dto.UpdateOrderStatusRequest;
import com.trackops.server.adapters.input.web.dto.OrderResponse;
import com.trackops.server.adapters.input.web.dto.OrderListResponse;
import com.trackops.server.domain.model.enums.OrderStatus;

import java.util.UUID;
import java.util.List;

public interface OrderServicePort {

    // CRUD operations
    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse getOrderById(UUID orderId);
    OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus);
    OrderResponse cancelOrder(UUID orderId);
        
    // Query operations
    List<OrderResponse> getAllOrders();
    List<OrderResponse> getOrdersByStatus(OrderStatus status);
    List<OrderResponse> getOrdersByCustomerId(UUID customerId);
        
    // Business operations
    OrderResponse confirmOrder(UUID orderId);
    OrderResponse processOrder(UUID orderId);
    OrderResponse shipOrder(UUID orderId);
    OrderResponse deliverOrder(UUID orderId);

}