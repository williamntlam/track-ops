package com.trackops.server.adapters.input.web.controllers;

import com.trackops.server.adapters.output.messaging.DlqThrottleMonitor;
import com.trackops.server.application.services.dlq.DlqOrderService;
import com.trackops.server.domain.model.dlq.DlqOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for Dead Letter Queue management and monitoring.
 * DLQ is stored in PostgreSQL (dlq_orders table). Only active when Debezium strategy is enabled.
 */
@RestController
@RequestMapping("/api/dlq")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event-publishing.strategy", havingValue = "debezium")
public class DeadLetterQueueController {

    private final DlqOrderService dlqOrderService;
    private final Optional<DlqThrottleMonitor> throttleMonitor;

    /**
     * Get DLQ metrics from PostgreSQL dlq_orders. Includes throttle status when throttling is enabled.
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getDLQMetrics() {
        long pendingCount = dlqOrderService.countPending();
        long totalCount = dlqOrderService.countTotal();

        Map<String, Object> response = new HashMap<>(Map.of(
                "pendingDlqCount", pendingCount,
                "totalDlqCount", totalCount,
                "status", totalCount > 0 ? "ALERT" : "HEALTHY"
        ));
        throttleMonitor.ifPresent(m -> {
            response.put("consumerPausedByThrottle", m.isConsumerPausedByThrottle());
        });

        return ResponseEntity.ok(response);
    }

    /**
     * Get DLQ health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getDLQHealth() {
        long pendingCount = dlqOrderService.countPending();
        boolean isHealthy = pendingCount == 0;

        Map<String, Object> response = Map.of(
                "status", isHealthy ? "UP" : "DOWN",
                "totalDlqCount", pendingCount,
                "message", isHealthy ? "No messages in DLQ" : "Messages detected in DLQ"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * List failed order events in the DLQ (PostgreSQL). Query by status or orderId.
     */
    @GetMapping("/orders")
    public ResponseEntity<List<DlqOrder>> listDlqOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderId) {
        List<DlqOrder> list;
        if (orderId != null && !orderId.isBlank()) {
            list = dlqOrderService.findByOrderId(orderId);
        } else if (status != null && !status.isBlank()) {
            list = dlqOrderService.findByStatus(status);
        } else {
            list = dlqOrderService.findByStatus("PENDING");
        }
        return ResponseEntity.ok(list);
    }

    /**
     * Get a single DLQ order entry by id.
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<DlqOrder> getDlqOrder(@PathVariable UUID id) {
        return dlqOrderService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
