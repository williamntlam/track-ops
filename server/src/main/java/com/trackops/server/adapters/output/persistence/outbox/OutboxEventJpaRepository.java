package com.trackops.server.adapters.output.persistence.outbox;

import com.trackops.server.domain.model.outbox.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, UUID> {
    
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();
    
    List<OutboxEvent> findByAggregateId(String aggregateId);
    
    List<OutboxEvent> findByEventType(String eventType);
    
    List<OutboxEvent> findByProcessedFalseAndRetryCountLessThan(int maxRetries);
    
    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.processed = true AND o.processedAt < :cutoffTime")
    void deleteByProcessedTrueAndProcessedAtBefore(@Param("cutoffTime") Instant cutoffTime);
    
    long countByProcessedFalse();
    
    long countByProcessedTrue();
}
