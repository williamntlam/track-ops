package com.trackops.server.ports.output.cache;

import com.trackops.server.domain.model.CacheOperationResult;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.adapters.input.web.dto.OrderResponse;
import org.springframework.data.domain.Page;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderCachePort {

    // Single Order Operations
    CacheOperationResult cacheOrder(Order order, Duration ttl);
    Optional<Order> getOrder(UUID orderId);
    CacheOperationResult removeOrder(UUID orderId);
    boolean hasOrder(UUID orderId);
    CacheOperationResult updateOrder(Order order, Duration ttl);

    // Order Response Operations (for API responses)
    CacheOperationResult cacheOrderResponse(UUID orderId, OrderResponse response, Duration ttl);
    Optional<OrderResponse> getOrderResponse(UUID orderId);
    /** Remaining TTL for order response key; empty if key missing or has no expiry. Used for probabilistic refresh. */
    Optional<Duration> getOrderResponseRemainingTtl(UUID orderId);
    /** Remaining TTL for order entity key; empty if key missing or has no expiry. Used for probabilistic refresh. */
    Optional<Duration> getOrderRemainingTtl(UUID orderId);
    CacheOperationResult removeOrderResponse(UUID orderId);
    CacheOperationResult updateOrderResponse(UUID orderId, OrderResponse response, Duration ttl);

    // Bulk Operations
    CacheOperationResult cacheOrdersByStatus(String status, List<OrderResponse> orders, Duration ttl);
    Optional<List<OrderResponse>> getOrdersByStatus(String status);
    CacheOperationResult removeOrdersByStatus(String status);

    CacheOperationResult cacheOrdersByCustomer(UUID customerId, List<OrderResponse> orders, Duration ttl);
    Optional<List<OrderResponse>> getOrdersByCustomer(UUID customerId);
    CacheOperationResult removeOrdersByCustomer(UUID customerId);

    // Pagination Cache (optional - can be complex)
    CacheOperationResult cacheOrderPage(String pageKey, Page<OrderResponse> page, Duration ttl);
    Optional<Page<OrderResponse>> getOrderPage(String pageKey);
    CacheOperationResult removeOrderPage(String pageKey);

    // Cache Invalidation
    CacheOperationResult invalidateAllOrderCaches(UUID orderId);
    CacheOperationResult invalidateCustomerOrderCaches(UUID customerId);
    CacheOperationResult invalidateStatusOrderCaches(String status);
}
