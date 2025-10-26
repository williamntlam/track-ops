package com.trackops.server.ports.output.persistence;

import com.trackops.server.domain.model.outbox.OutboxEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAt();
    
    Page<OutboxEvent> findUnprocessedEventsOrderByCreatedAt(Pageable pageable);
    
    @Query("SELECT o FROM OutboxEvent o WHERE o.processed = false AND o.retryCount < o.maxRetries ORDER BY o.createdAt")
    List<OutboxEvent> findRetryableEvents();
    
    @Query("SELECT o FROM OutboxEvent o WHERE o.processed = false AND o.createdAt < ?1")
    List<OutboxEvent> findStaleEvents(LocalDateTime cutoffTime);
    
    long countByProcessedFalse();
}
