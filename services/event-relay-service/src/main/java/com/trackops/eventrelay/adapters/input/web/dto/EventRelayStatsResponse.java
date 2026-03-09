package com.trackops.eventrelay.adapters.input.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRelayStatsResponse {
    
    private long unprocessedCount;
    private long processedCount;
    private long failedCount;
    private long totalCount;
    private String serviceStatus;
    private long timestamp;
    
    public static EventRelayStatsResponse from(com.trackops.eventrelay.application.services.EventRelayService.EventRelayStats stats) {
        return EventRelayStatsResponse.builder()
            .unprocessedCount(stats.getUnprocessedCount())
            .processedCount(stats.getProcessedCount())
            .failedCount(stats.getFailedCount())
            .totalCount(stats.getTotalCount())
            .serviceStatus("UP")
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
