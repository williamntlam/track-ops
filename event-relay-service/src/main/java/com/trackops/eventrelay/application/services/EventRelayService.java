package com.trackops.eventrelay.application.services;

import com.trackops.eventrelay.config.AvroEventConverter;
import com.trackops.eventrelay.domain.model.OutboxEvent;
import com.trackops.eventrelay.ports.output.persistence.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class EventRelayService {
    
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<UUID, GenericRecord> kafkaTemplate;
    private final AvroEventConverter avroEventConverter;
    
    @Value("${event-relay.batch-size:10}")
    private int batchSize;
    
    @Value("${event-relay.max-retries:3}")
    private int maxRetries;
    
    @Value("${event-relay.retry-delay:1000}")
    private long retryDelay;
    
    public EventRelayService(OutboxEventRepository outboxEventRepository, 
                           KafkaTemplate<UUID, GenericRecord> kafkaTemplate,
                           AvroEventConverter avroEventConverter) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.avroEventConverter = avroEventConverter;
    }
    
    /**
     * Scheduled task to process outbox events
     * Runs every 5 seconds by default
     */
    @Scheduled(fixedDelayString = "${event-relay.polling-interval:5000}")
    @Transactional
    public void processOutboxEvents() {
        try {
            List<OutboxEvent> unprocessedEvents = outboxEventRepository
                .findRetryableEventsOrderByCreatedAt();
            
            if (unprocessedEvents.isEmpty()) {
                log.debug("No retryable events found");
                return;
            }
            
            // Process events in batches
            int processedCount = 0;
            for (OutboxEvent event : unprocessedEvents) {
                if (processedCount >= batchSize) {
                    break;
                }
                
                try {
                    processEvent(event);
                    processedCount++;
                } catch (Exception e) {
                    log.error("Failed to process event {}: {}", event.getId(), e.getMessage(), e);
                    // Continue with next event
                }
            }
            
            if (processedCount > 0) {
                log.info("Processed {} outbox events", processedCount);
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
            
            // Convert JSON payload to Avro GenericRecord
            GenericRecord avroRecord = avroEventConverter.jsonToAvro(topic, payload);
            
            // Send message to Kafka
            // The Confluent Avro serializer will automatically register the schema
            // if it doesn't exist and validate compatibility
            kafkaTemplate.send(topic, key, avroRecord)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to send event {} to Kafka: {}", event.getId(), throwable.getMessage());
                        // Mark as failed for retry
                        event.markAsFailed(throwable.getMessage());
                        outboxEventRepository.save(event);
                    } else {
                        log.debug("Successfully sent event {} to Kafka topic {}", event.getId(), topic);
                    }
                });
            
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
     * Clean up old processed events
     * Runs every hour by default
     */
    @Scheduled(fixedDelay = 3600000) // 1 hour
    @Transactional
    public void cleanupProcessedEvents() {
        try {
            if (!Boolean.parseBoolean(System.getProperty("event-relay.cleanup.enabled", "true"))) {
                return;
            }
            
            int retentionHours = Integer.parseInt(System.getProperty("event-relay.cleanup.retention-hours", "24"));
            Instant cutoffTime = Instant.now().minusSeconds(retentionHours * 3600);
            
            List<OutboxEvent> oldEvents = outboxEventRepository.findProcessedEventsBefore(cutoffTime);
            
            if (!oldEvents.isEmpty()) {
                outboxEventRepository.deleteAll(oldEvents);
                log.info("Cleaned up {} processed events older than {} hours", 
                        oldEvents.size(), retentionHours);
            }
            
        } catch (Exception e) {
            log.error("Error cleaning up processed events: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get event relay statistics
     */
    public EventRelayStats getStats() {
        long unprocessedCount = outboxEventRepository.countUnprocessedEvents();
        long processedCount = outboxEventRepository.countProcessedEvents();
        long failedCount = outboxEventRepository.countFailedEvents();
        
        return new EventRelayStats(unprocessedCount, processedCount, failedCount);
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
     * Statistics class for event relay
     */
    public static class EventRelayStats {
        private final long unprocessedCount;
        private final long processedCount;
        private final long failedCount;

        public EventRelayStats(long unprocessedCount, long processedCount, long failedCount) {
            this.unprocessedCount = unprocessedCount;
            this.processedCount = processedCount;
            this.failedCount = failedCount;
        }

        public long getUnprocessedCount() { return unprocessedCount; }
        public long getProcessedCount() { return processedCount; }
        public long getFailedCount() { return failedCount; }
        public long getTotalCount() { return unprocessedCount + processedCount + failedCount; }
    }
}
