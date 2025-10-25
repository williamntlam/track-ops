package com.trackops.eventrelay.ports.output.persistence;

import com.trackops.eventrelay.domain.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnprocessedEventsOrderByCreatedAt();
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false AND e.retryCount < e.maxRetries ORDER BY e.createdAt ASC")
    List<OutboxEvent> findRetryableEventsOrderByCreatedAt();
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = true AND e.processedAt < :cutoffTime")
    List<OutboxEvent> findProcessedEventsBefore(@Param("cutoffTime") Instant cutoffTime);
    
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.processed = false")
    long countUnprocessedEvents();
    
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.processed = true")
    long countProcessedEvents();
    
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.processed = false AND e.retryCount >= e.maxRetries")
    long countFailedEvents();
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.aggregateId = :aggregateId ORDER BY e.createdAt ASC")
    List<OutboxEvent> findByAggregateId(@Param("aggregateId") String aggregateId);
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.eventType = :eventType ORDER BY e.createdAt ASC")
    List<OutboxEvent> findByEventType(@Param("eventType") String eventType);
}
