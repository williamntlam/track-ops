package com.trackops.server.domain.model.outbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "processed", nullable = false)
    private Boolean processed = false;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    
    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "partition_key")
    private String partitionKey;
    
    @Column(name = "version", nullable = false)
    private Long version = 0L;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    // Business methods
    public boolean canRetry() {
        return retryCount < maxRetries;
    }
    
    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
        this.errorMessage = null;
    }
    
    public void markAsFailed(String errorMessage) {
        this.retryCount++;
        this.errorMessage = errorMessage;
        if (!canRetry()) {
            this.processed = true;
            this.processedAt = LocalDateTime.now();
        }
    }
    
    public boolean isProcessed() {
        return processed;
    }
}