package com.trackops.eventrelay.adapters.input.web.dto;

import com.trackops.eventrelay.domain.model.OutboxEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventResponse {
    
    private UUID id;
    private String aggregateId;
    private String eventType;
    private String payload;
    private Instant createdAt;
    private boolean processed;
    private Instant processedAt;
    private int retryCount;
    private int maxRetries;
    private String errorMessage;
    private String partitionKey;
    private Long version;
    
    public static OutboxEventResponse from(OutboxEvent event) {
        return OutboxEventResponse.builder()
            .id(event.getId())
            .aggregateId(event.getAggregateId())
            .eventType(event.getEventType())
            .payload(event.getPayload())
            .createdAt(event.getCreatedAt())
            .processed(event.isProcessed())
            .processedAt(event.getProcessedAt())
            .retryCount(event.getRetryCount())
            .maxRetries(event.getMaxRetries())
            .errorMessage(event.getErrorMessage())
            .partitionKey(event.getPartitionKey())
            .version(event.getVersion())
            .build();
    }
}
