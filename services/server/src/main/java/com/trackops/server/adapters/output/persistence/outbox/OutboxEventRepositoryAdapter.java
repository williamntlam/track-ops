package com.trackops.server.adapters.output.persistence.outbox;

import com.trackops.server.domain.model.outbox.OutboxEvent;
import com.trackops.server.ports.output.persistence.outbox.OutboxEventRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {
    
    private final OutboxEventJpaRepository outboxEventJpaRepository;

    public OutboxEventRepositoryAdapter(OutboxEventJpaRepository outboxEventJpaRepository) {
        this.outboxEventJpaRepository = outboxEventJpaRepository;
    }

    @Override
    public OutboxEvent save(OutboxEvent outboxEvent) {
        return outboxEventJpaRepository.save(outboxEvent);
    }

    @Override
    public Optional<OutboxEvent> findById(UUID id) {
        return outboxEventJpaRepository.findById(id);
    }

    @Override
    public List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc() {
        return outboxEventJpaRepository.findByProcessedFalseOrderByCreatedAtAsc();
    }

    @Override
    public List<OutboxEvent> findByAggregateId(String aggregateId) {
        return outboxEventJpaRepository.findByAggregateId(aggregateId);
    }

    @Override
    public List<OutboxEvent> findByEventType(String eventType) {
        return outboxEventJpaRepository.findByEventType(eventType);
    }

    @Override
    public List<OutboxEvent> findByProcessedFalseAndRetryCountLessThan(int maxRetries) {
        return outboxEventJpaRepository.findByProcessedFalseAndRetryCountLessThan(maxRetries);
    }

    @Override
    public void deleteById(UUID id) {
        outboxEventJpaRepository.deleteById(id);
    }

    @Override
    public void deleteByProcessedTrueAndProcessedAtBefore(Instant cutoffTime) {
        outboxEventJpaRepository.deleteByProcessedTrueAndProcessedAtBefore(cutoffTime);
    }

    @Override
    public boolean existsById(UUID id) {
        return outboxEventJpaRepository.existsById(id);
    }

    @Override
    public long countByProcessedFalse() {
        return outboxEventJpaRepository.countByProcessedFalse();
    }

    @Override
    public long countByProcessedTrue() {
        return outboxEventJpaRepository.countByProcessedTrue();
    }
}
