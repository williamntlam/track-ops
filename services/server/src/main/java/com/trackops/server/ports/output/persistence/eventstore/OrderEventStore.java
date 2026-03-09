package com.trackops.server.ports.output.persistence.eventstore;

import com.trackops.server.domain.model.eventstore.OrderEvent;

import java.util.List;
import java.util.UUID;

/**
 * Port for appending to and reading from the order event store.
 * Implemented via Spring Data JPA (no raw JDBC) to avoid connection stalling.
 */
public interface OrderEventStore {

    /**
     * Append an event for an order. Sequence number is assigned in a single transaction.
     *
     * @param orderId       order aggregate id
     * @param eventType     e.g. ORDER_CREATED, ORDER_CANCELLED
     * @param payloadJson   JSON string (stored as JSONB)
     * @param schemaVersion version of the payload structure
     * @return the persisted OrderEvent
     */
    OrderEvent append(UUID orderId, String eventType, String payloadJson, int schemaVersion);

    /**
     * Load all events for an order in sequence order.
     */
    List<OrderEvent> findByOrderIdOrderBySequenceNumberAsc(UUID orderId);
}
