package com.trackops.server.application.services.outbox;

import com.trackops.server.domain.model.outbox.OutboxEvent;
import com.trackops.server.ports.output.persistence.outbox.OutboxEventRepository;
import com.trackops.server.ports.output.events.orders.OrderEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OutboxEventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);
    
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<UUID, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, 
                               KafkaTemplate<UUID, String> kafkaTemplate,
                               ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Scheduled task to process outbox events
     * Runs every 5 seconds
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        try {
            List<OutboxEvent> unprocessedEvents = outboxEventRepository
                .findByProcessedFalseOrderByCreatedAtAsc();
            
            if (!unprocessedEvents.isEmpty()) {
                log.debug("Processing {} outbox events", unprocessedEvents.size());
                
                for (OutboxEvent event : unprocessedEvents) {
                    processEvent(event);
                }
            }
        } catch (Exception e) {
            log.error("Error processing outbox events: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a single outbox event
     */
    @Transactional
    public void processEvent(OutboxEvent event) {
        try {
            log.debug("Processing outbox event: {} for aggregate: {}", 
                     event.getId(), event.getAggregateId());
            
            // Check if event can be retried
            if (!event.canRetry()) {
                log.warn("Outbox event {} has exceeded max retries, marking as failed", event.getId());
                event.markAsFailed("Max retries exceeded");
                outboxEventRepository.save(event);
                return;
            }
            
            // Publish to Kafka
            String topic = event.getEventType();
            UUID key = UUID.fromString(event.getAggregateId());
            String payload = event.getPayload();
            
            kafkaTemplate.send(topic, key, payload);
            
            // Mark as processed
            event.markAsProcessed();
            outboxEventRepository.save(event);
            
            log.info("Successfully published outbox event {} to topic {}", 
                    event.getId(), topic);
            
        } catch (Exception e) {
            log.error("Failed to process outbox event {}: {}", 
                     event.getId(), e.getMessage(), e);
            
            // Mark as failed and increment retry count
            event.markAsFailed(e.getMessage());
            outboxEventRepository.save(event);
        }
    }

    /**
     * Manually process a specific outbox event
     */
    @Transactional
    public boolean processEventById(UUID eventId) {
        try {
            OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Outbox event not found: " + eventId));
            
            if (event.isProcessed()) {
                log.warn("Outbox event {} is already processed", eventId);
                return true;
            }
            
            processEvent(event);
            return event.isProcessed();
            
        } catch (Exception e) {
            log.error("Error processing outbox event {}: {}", eventId, e.getMessage(), e);
            return false;
        }
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
     * Clean up old processed events
     * Runs every hour
     */
    @Scheduled(fixedDelay = 3600000) // 1 hour
    @Transactional
    public void cleanupProcessedEvents() {
        try {
            java.time.Instant cutoffTime = java.time.Instant.now().minusSeconds(86400); // 24 hours ago
            outboxEventRepository.deleteByProcessedTrueAndProcessedAtBefore(cutoffTime);
            log.info("Cleaned up processed outbox events older than 24 hours");
        } catch (Exception e) {
            log.error("Error cleaning up processed events: {}", e.getMessage(), e);
        }
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
