package com.trackops.server.ports.output.persistence.outbox;

import com.trackops.server.domain.model.outbox.OutboxEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository {
    
    OutboxEvent save(OutboxEvent outboxEvent);
    
    Optional<OutboxEvent> findById(UUID id);
    
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();
    
    List<OutboxEvent> findByAggregateId(String aggregateId);
    
    List<OutboxEvent> findByEventType(String eventType);
    
    List<OutboxEvent> findByProcessedFalseAndRetryCountLessThan(int maxRetries);
    
    void deleteById(UUID id);
    
    void deleteByProcessedTrueAndProcessedAtBefore(java.time.Instant cutoffTime);
    
    boolean existsById(UUID id);
    
    long countByProcessedFalse();
    
    long countByProcessedTrue();
}
