package com.trackops.server.adapters.output.persistence.events;

import com.trackops.server.domain.model.events.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEvent, String> {

    Optional<ProcessedEvent> findByEventId(String eventId);

    List<ProcessedEvent> findByOrderId(Long orderId);

    /**
     * Idempotent insert: inserts a row only if event_id is not already present.
     * Uses ON CONFLICT (event_id) DO NOTHING so redelivered messages do not create duplicates.
     * Matches V2 schema: event_id UUID, order_id UUID, event_type, processed_at, success, consumer_group, offset.
     *
     * @return number of rows inserted (0 = already processed, 1 = new)
     */
    @Modifying
    @Query(value = """
        INSERT INTO processed_events (event_id, order_id, event_type, processed_at, success, consumer_group, "offset")
        VALUES (CAST(:eventId AS uuid), CAST(:orderId AS uuid), :eventType, CURRENT_TIMESTAMP, true, :consumerGroup, COALESCE(:offsetVal, 0))
        ON CONFLICT (event_id) DO NOTHING
        """, nativeQuery = true)
    int insertOnConflictDoNothing(
            @Param("eventId") String eventId,
            @Param("orderId") String orderId,
            @Param("eventType") String eventType,
            @Param("consumerGroup") String consumerGroup,
            @Param("offsetVal") Long offsetVal);
}
