package com.trackops.server.ports.output.cache;

import com.trackops.server.domain.model.CacheOperationResult;
import com.trackops.server.domain.model.enums.OrderStatus;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface OrderStatusCachePort {

    CacheOperationResult cacheOrderStatus(UUID orderId, OrderStatus status, Duration ttl);
    Optional<OrderStatus> getOrderStatus(UUID orderId);
    CacheOperationResult removeOrderStatus(UUID orderId);
    boolean hasOrderStatus(UUID orderId);
    CacheOperationResult updateOrderStatus(UUID orderId, OrderStatus newStatus, Duration ttl);

}