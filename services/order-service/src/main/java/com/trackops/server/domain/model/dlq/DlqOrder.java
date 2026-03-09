package com.trackops.server.domain.model.dlq;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Failed order event stored for retry and analysis (replaces Kafka DLQ for order events).
 */
@Entity
@Table(name = "dlq_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlqOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "message_type", length = 100)
    private String messageType;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    public void markRetryAttempt(String errorLog) {
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
        this.errorLog = errorLog;
        if (!canRetry()) {
            this.status = "PERMANENT_FAILURE";
        }
    }

    public void markCompleted() {
        this.status = "COMPLETED";
    }

    public void markProcessing() {
        this.status = "PROCESSING";
    }
}
