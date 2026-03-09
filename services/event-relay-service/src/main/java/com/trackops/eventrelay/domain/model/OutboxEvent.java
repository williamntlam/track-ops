package com.trackops.eventrelay.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;
    
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId; // e.g., orderId
    
    @Column(name = "event_type", nullable = false)
    private String eventType; // e.g., "ORDER_CREATED", "ORDER_STATUS_UPDATED"
    
    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload; // JSON serialized event
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "processed", nullable = false)
    private boolean processed = false;
    
    @Column(name = "processed_at")
    private Instant processedAt;
    
    @Column(name = "retry_count")
    private int retryCount = 0;
    
    @Column(name = "max_retries")
    private int maxRetries = 3;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "partition_key")
    private String partitionKey; // For Kafka partitioning
    
    @Version
    private Long version;

    // Constructors
    public OutboxEvent() {}

    public OutboxEvent(String aggregateId, String eventType, String payload, String partitionKey) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.partitionKey = partitionKey;
        this.createdAt = Instant.now();
        this.processed = false;
        this.retryCount = 0;
        this.maxRetries = 3;
    }

    // Business methods
    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = Instant.now();
        this.errorMessage = null;
    }

    public void markAsFailed(String errorMessage) {
        this.retryCount++;
        this.errorMessage = errorMessage;
        this.processed = false;
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetries;
    }

    public boolean isProcessed() {
        return this.processed;
    }

    public boolean isFailed() {
        return !this.processed && this.retryCount >= this.maxRetries;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public void setProcessed(boolean processed) { this.processed = processed; }
    
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public String getPartitionKey() { return partitionKey; }
    public void setPartitionKey(String partitionKey) { this.partitionKey = partitionKey; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
