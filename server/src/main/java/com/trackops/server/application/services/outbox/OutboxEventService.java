package com.trackops.server.application.services.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.domain.model.outbox.OutboxEvent;
import com.trackops.server.ports.output.persistence.outbox.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxEventService {
    
    private static final Logger log = LoggerFactory.getLogger(OutboxEventService.class);
    
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Create and save an outbox event
     */
    @Transactional
    public OutboxEvent createEvent(String aggregateId, String eventType, Object eventData, String partitionKey) {
        try {
            String payload = objectMapper.writeValueAsString(eventData);
            
            OutboxEvent outboxEvent = new OutboxEvent(
                aggregateId,
                eventType,
                payload,
                partitionKey
            );
            
            OutboxEvent savedEvent = outboxEventRepository.save(outboxEvent);
            log.debug("Created outbox event {} for aggregate {}", savedEvent.getId(), aggregateId);
            
            return savedEvent;
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event data for aggregate {}: {}", aggregateId, e.getMessage(), e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }

    /**
     * Create outbox event for order events
     */
    @Transactional
    public OutboxEvent createOrderEvent(String orderId, String eventType, Object eventData) {
        return createEvent(orderId, eventType, eventData, orderId);
    }

    /**
     * Get all unprocessed events
     */
    public List<OutboxEvent> getUnprocessedEvents() {
        return outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
    }

    /**
     * Get events by aggregate ID
     */
    public List<OutboxEvent> getEventsByAggregateId(String aggregateId) {
        return outboxEventRepository.findByAggregateId(aggregateId);
    }

    /**
     * Get events by event type
     */
    public List<OutboxEvent> getEventsByEventType(String eventType) {
        return outboxEventRepository.findByEventType(eventType);
    }

    /**
     * Get failed events that can be retried
     */
    public List<OutboxEvent> getRetryableEvents() {
        return outboxEventRepository.findByProcessedFalseAndRetryCountLessThan(3);
    }

    /**
     * Get outbox event by ID
     */
    public OutboxEvent getEventById(UUID eventId) {
        return outboxEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Outbox event not found: " + eventId));
    }

    /**
     * Delete processed events older than specified time
     */
    @Transactional
    public void deleteOldProcessedEvents(Instant cutoffTime) {
        outboxEventRepository.deleteByProcessedTrueAndProcessedAtBefore(cutoffTime);
        log.info("Deleted processed events older than {}", cutoffTime);
    }

    /**
     * Get outbox event statistics
     */
    public OutboxEventStats getStats() {
        long unprocessedCount = outboxEventRepository.countByProcessedFalse();
        long processedCount = outboxEventRepository.countByProcessedTrue();
        
        return new OutboxEventStats(unprocessedCount, processedCount);
    }

    /**
     * Statistics class for outbox events
     */
    public static class OutboxEventStats {
        private final long unprocessedCount;
        private final long processedCount;

        public OutboxEventStats(long unprocessedCount, long processedCount) {
            this.unprocessedCount = unprocessedCount;
            this.processedCount = processedCount;
        }

        public long getUnprocessedCount() { return unprocessedCount; }
        public long getProcessedCount() { return processedCount; }
        public long getTotalCount() { return unprocessedCount + processedCount; }
    }
}
