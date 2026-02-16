package com.trackops.server.ports.output.cache;

import com.trackops.server.adapters.input.web.dto.OrderResponse;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for the order materialized view stored in Redis.
 * Enables sub-millisecond reads for GET /orders/{id} without hitting PostgreSQL.
 */
public interface OrderMaterializedViewPort {

    String VIEW_KEY_PREFIX = "order:view:";

    /**
     * Get the current materialized view for an order (e.g. from Redis).
     */
    Optional<OrderResponse> getView(UUID orderId);

    /**
     * Write or update the materialized view with a TTL (e.g. 24 hours).
     */
    void putView(UUID orderId, OrderResponse view, Duration ttl);

    /**
     * Remove the view (e.g. on order cancellation or cache invalidation).
     */
    void removeView(UUID orderId);
}
