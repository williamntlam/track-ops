package com.trackops.server.adapters.input.web.controllers;

import com.trackops.server.adapters.output.monitoring.DeadLetterQueueMonitor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for Dead Letter Queue management and monitoring.
 * Only active when Debezium strategy is enabled.
 */
@RestController
@RequestMapping("/api/v1/dlq")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event-publishing.strategy", havingValue = "debezium")
public class DeadLetterQueueController {
    
    private final DeadLetterQueueMonitor dlqMonitor;
    
    /**
     * Get DLQ metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getDLQMetrics() {
        DeadLetterQueueMonitor.DLQMetrics metrics = dlqMonitor.getMetrics();
        
        Map<String, Object> response = Map.of(
            "debeziumOrderEventDlqCount", metrics.getDebeziumOrderEventDlqCount(),
            "debeziumCacheConsumerDlqCount", metrics.getDebeziumCacheConsumerDlqCount(),
            "debeziumCacheWarmerDlqCount", metrics.getDebeziumCacheWarmerDlqCount(),
            "totalDlqCount", metrics.getTotalDlqCount(),
            "status", metrics.getTotalDlqCount() > 0 ? "ALERT" : "HEALTHY"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get DLQ health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getDLQHealth() {
        DeadLetterQueueMonitor.DLQMetrics metrics = dlqMonitor.getMetrics();
        
        boolean isHealthy = metrics.getTotalDlqCount() == 0;
        
        Map<String, Object> response = Map.of(
            "status", isHealthy ? "UP" : "DOWN",
            "totalDlqCount", metrics.getTotalDlqCount(),
            "message", isHealthy ? "No messages in DLQ" : "Messages detected in DLQ"
        );
        
        return ResponseEntity.ok(response);
    }
}
