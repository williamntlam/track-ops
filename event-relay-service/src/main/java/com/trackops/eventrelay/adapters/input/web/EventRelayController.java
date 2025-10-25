package com.trackops.eventrelay.adapters.input.web;

import com.trackops.eventrelay.application.services.EventRelayService;
import com.trackops.eventrelay.domain.model.OutboxEvent;
import com.trackops.eventrelay.ports.output.persistence.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/event-relay")
public class EventRelayController {
    
    private final EventRelayService eventRelayService;
    private final OutboxEventRepository outboxEventRepository;
    
    public EventRelayController(EventRelayService eventRelayService,
                              OutboxEventRepository outboxEventRepository) {
        this.eventRelayService = eventRelayService;
        this.outboxEventRepository = outboxEventRepository;
    }
    
    /**
     * Get event relay statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<EventRelayService.EventRelayStats> getStats() {
        EventRelayService.EventRelayStats stats = eventRelayService.getStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get all unprocessed events
     */
    @GetMapping("/events/unprocessed")
    public ResponseEntity<List<OutboxEvent>> getUnprocessedEvents() {
        List<OutboxEvent> events = outboxEventRepository.findUnprocessedEventsOrderByCreatedAt();
        return ResponseEntity.ok(events);
    }
    
    /**
     * Get events by aggregate ID
     */
    @GetMapping("/events/aggregate/{aggregateId}")
    public ResponseEntity<List<OutboxEvent>> getEventsByAggregateId(@PathVariable String aggregateId) {
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(aggregateId);
        return ResponseEntity.ok(events);
    }
    
    /**
     * Get events by event type
     */
    @GetMapping("/events/type/{eventType}")
    public ResponseEntity<List<OutboxEvent>> getEventsByType(@PathVariable String eventType) {
        List<OutboxEvent> events = outboxEventRepository.findByEventType(eventType);
        return ResponseEntity.ok(events);
    }
    
    /**
     * Get a specific event by ID
     */
    @GetMapping("/events/{eventId}")
    public ResponseEntity<OutboxEvent> getEventById(@PathVariable UUID eventId) {
        return outboxEventRepository.findById(eventId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Manually process a specific event
     */
    @PostMapping("/events/{eventId}/process")
    public ResponseEntity<String> processEvent(@PathVariable UUID eventId) {
        try {
            boolean success = eventRelayService.processEventById(eventId);
            if (success) {
                return ResponseEntity.ok("Event processed successfully");
            } else {
                return ResponseEntity.badRequest().body("Failed to process event");
            }
        } catch (Exception e) {
            log.error("Error processing event {}: {}", eventId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing event: " + e.getMessage());
        }
    }
    
    /**
     * Trigger manual processing of all unprocessed events
     */
    @PostMapping("/process-all")
    public ResponseEntity<String> processAllEvents() {
        try {
            eventRelayService.processOutboxEvents();
            return ResponseEntity.ok("Processing triggered for all unprocessed events");
        } catch (Exception e) {
            log.error("Error processing all events: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing events: " + e.getMessage());
        }
    }
    
    /**
     * Get health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        EventRelayService.EventRelayStats stats = eventRelayService.getStats();
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "stats", stats,
            "timestamp", System.currentTimeMillis()
        ));
    }
}
