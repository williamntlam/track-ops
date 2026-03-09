package com.trackops.server.adapters.input.web.controllers;

import com.trackops.server.application.services.outbox.OutboxEventService;
import com.trackops.server.application.services.outbox.OutboxEventPublisher;
import com.trackops.server.domain.model.outbox.OutboxEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/outbox")
@Validated
public class OutboxController {
    
    private final OutboxEventService outboxEventService;
    private final OutboxEventPublisher outboxEventPublisher;

    public OutboxController(OutboxEventService outboxEventService, OutboxEventPublisher outboxEventPublisher) {
        this.outboxEventService = outboxEventService;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @GetMapping("/events")
    public ResponseEntity<List<OutboxEvent>> getUnprocessedEvents() {
        try {
            List<OutboxEvent> events = outboxEventService.getUnprocessedEvents();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/events/aggregate/{aggregateId}")
    public ResponseEntity<List<OutboxEvent>> getEventsByAggregateId(
            @PathVariable @NotBlank(message = "Aggregate ID is required") 
            @Pattern(regexp = "^[A-Za-z0-9\\-_]{1,50}$", message = "Aggregate ID must be 1-50 characters and contain only letters, numbers, hyphens, and underscores") 
            String aggregateId) {
        try {
            List<OutboxEvent> events = outboxEventService.getEventsByAggregateId(aggregateId);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/events/type/{eventType}")
    public ResponseEntity<List<OutboxEvent>> getEventsByEventType(@PathVariable String eventType) {
        try {
            List<OutboxEvent> events = outboxEventService.getEventsByEventType(eventType);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/events/retryable")
    public ResponseEntity<List<OutboxEvent>> getRetryableEvents() {
        try {
            List<OutboxEvent> events = outboxEventService.getRetryableEvents();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<OutboxEvent> getEventById(@PathVariable String eventId) {
        try {
            OutboxEvent event = outboxEventService.getEventById(UUID.fromString(eventId));
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/events/{eventId}/process")
    public ResponseEntity<String> processEvent(@PathVariable String eventId) {
        try {
            boolean success = outboxEventPublisher.processEventById(UUID.fromString(eventId));
            if (success) {
                return ResponseEntity.ok("Event processed successfully");
            } else {
                return ResponseEntity.badRequest().body("Failed to process event");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing event: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<OutboxEventService.OutboxEventStats> getStats() {
        try {
            OutboxEventService.OutboxEventStats stats = outboxEventService.getStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/cleanup")
    public ResponseEntity<String> cleanupOldEvents() {
        try {
            java.time.Instant cutoffTime = java.time.Instant.now().minusSeconds(86400); // 24 hours ago
            outboxEventService.deleteOldProcessedEvents(cutoffTime);
            return ResponseEntity.ok("Cleanup completed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error during cleanup: " + e.getMessage());
        }
    }
}
