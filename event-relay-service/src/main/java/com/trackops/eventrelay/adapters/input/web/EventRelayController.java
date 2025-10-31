package com.trackops.eventrelay.adapters.input.web;

import com.trackops.eventrelay.adapters.input.web.dto.*;
import com.trackops.eventrelay.application.services.EventRelayService;
import com.trackops.eventrelay.domain.model.OutboxEvent;
import com.trackops.eventrelay.ports.output.persistence.OutboxEventRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventRelayController {
    
    private final EventRelayService eventRelayService;
    private final OutboxEventRepository outboxEventRepository;
    
    /**
     * Get event relay statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<EventRelayStatsResponse> getStats() {
        try {
            EventRelayService.EventRelayStats stats = eventRelayService.getStats();
            EventRelayStatsResponse response = EventRelayStatsResponse.from(stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving event relay statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get all unprocessed events with pagination
     */
    @GetMapping("/unprocessed")
    public ResponseEntity<Page<OutboxEventResponse>> getUnprocessedEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<OutboxEvent> events = outboxEventRepository.findUnprocessedEventsOrderByCreatedAt(pageable);
            Page<OutboxEventResponse> response = events.map(OutboxEventResponse::from);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving unprocessed events: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get events by aggregate ID
     */
    @GetMapping("/aggregate/{aggregateId}")
    public ResponseEntity<List<OutboxEventResponse>> getEventsByAggregateId(@PathVariable String aggregateId) {
        try {
            List<OutboxEvent> events = outboxEventRepository.findByAggregateId(aggregateId);
            List<OutboxEventResponse> response = events.stream()
                .map(OutboxEventResponse::from)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving events for aggregate {}: {}", aggregateId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get events by event type
     */
    @GetMapping("/type/{eventType}")
    public ResponseEntity<List<OutboxEventResponse>> getEventsByType(@PathVariable String eventType) {
        try {
            List<OutboxEvent> events = outboxEventRepository.findByEventType(eventType);
            List<OutboxEventResponse> response = events.stream()
                .map(OutboxEventResponse::from)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving events for type {}: {}", eventType, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get a specific event by ID
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<OutboxEventResponse> getEventById(@PathVariable UUID eventId) {
        try {
            return outboxEventRepository.findById(eventId)
                .map(OutboxEventResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving event {}: {}", eventId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Manually process a specific event
     */
    @PostMapping("/{eventId}/process")
    public ResponseEntity<Map<String, Object>> processEvent(@PathVariable UUID eventId) {
        try {
            boolean success = eventRelayService.processEventById(eventId);
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "Event processed successfully" : "Failed to process event",
                "eventId", eventId,
                "timestamp", System.currentTimeMillis()
            );
            
            return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error processing event {}: {}", eventId, e.getMessage(), e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "Error processing event: " + e.getMessage(),
                "eventId", eventId,
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Trigger manual processing of all unprocessed events
     */
    @PostMapping("/process-all")
    public ResponseEntity<Map<String, Object>> processAllEvents(@Valid @RequestBody(required = false) ProcessAllEventsRequest request) {
        try {
            if (request == null) {
                request = ProcessAllEventsRequest.builder().build();
            }
            
            eventRelayService.processOutboxEvents();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Processing triggered for all unprocessed events",
                "batchSize", request.getBatchSize(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing all events: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "Error processing events: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Get health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        try {
            EventRelayService.EventRelayStats stats = eventRelayService.getStats();
            Map<String, Object> healthStatus = Map.of(
                "status", "UP",
                "service", "event-relay-service",
                "version", "1.0.0",
                "stats", EventRelayStatsResponse.from(stats),
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.ok(healthStatus);
        } catch (Exception e) {
            log.error("Error retrieving health status: {}", e.getMessage(), e);
            Map<String, Object> errorStatus = Map.of(
                "status", "DOWN",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.status(500).body(errorStatus);
        }
    }
    
    /**
     * Simple ping endpoint
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        Map<String, String> response = Map.of(
            "status", "UP",
            "message", "Event relay service is running"
        );
        return ResponseEntity.ok(response);
    }
}
