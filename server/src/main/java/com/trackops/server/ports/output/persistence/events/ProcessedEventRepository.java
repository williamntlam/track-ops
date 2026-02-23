package com.trackops.server.ports.output.persistence.events;

import com.trackops.server.domain.model.events.ProcessedEvent;

import java.util.Optional;

public interface ProcessedEventRepository {

    ProcessedEvent save(ProcessedEvent processedEvent);
    Optional<ProcessedEvent> findByEventId(String eventId);
    Optional<ProcessedEvent> findByOrderId(Long orderId);
    boolean existsByEventId(String eventId);
    void deleteByEventId(String eventId);

    /**
     * Idempotent insert: ON CONFLICT (event_id) DO NOTHING.
     * @return number of rows inserted (0 = already processed, 1 = new)
     */
    int insertOnConflictDoNothing(String eventId, String orderId, String eventType, String consumerGroup, Long offsetVal);
}