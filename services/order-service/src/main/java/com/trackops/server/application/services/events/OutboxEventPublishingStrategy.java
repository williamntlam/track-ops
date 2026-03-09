package com.trackops.server.application.services.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.model.outbox.OutboxEvent;
import com.trackops.server.ports.output.persistence.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Outbox Pattern implementation for event publishing.
 * Publishes events to outbox table for reliable delivery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublishingStrategy implements EventPublishingStrategy {
    
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${app.event-publishing.strategy:outbox}")
    private String eventPublishingStrategy;
    
    @Override
    public void publishOrderCreated(Order order) {
        if (!isEnabled()) {
            log.debug("Outbox strategy disabled, skipping order created event");
            return;
        }
        
        try {
            String payload = objectMapper.writeValueAsString(order);
            OutboxEvent event = OutboxEvent.builder()
                .aggregateId(order.getId().toString())
                .eventType("ORDER_CREATED")
                .payload(payload)
                .partitionKey(order.getId().toString())
                .build();
            
            outboxEventRepository.save(event);
            log.info("Published ORDER_CREATED event for order: {}", order.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order for event publishing", e);
            throw new RuntimeException("Failed to publish order created event", e);
        }
    }
    
    @Override
    public void publishOrderStatusUpdated(Order order, String previousStatus) {
        if (!isEnabled()) {
            log.debug("Outbox strategy disabled, skipping order status updated event");
            return;
        }
        
        try {
            String payload = objectMapper.writeValueAsString(new OrderStatusUpdateEvent(
                order.getId().toString(),
                order.getStatus().toString(),
                previousStatus,
                order.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            ));
            
            OutboxEvent event = OutboxEvent.builder()
                .aggregateId(order.getId().toString())
                .eventType("ORDER_STATUS_UPDATED")
                .payload(payload)
                .partitionKey(order.getId().toString())
                .build();
            
            outboxEventRepository.save(event);
            log.info("Published ORDER_STATUS_UPDATED event for order: {}", order.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order status update for event publishing", e);
            throw new RuntimeException("Failed to publish order status updated event", e);
        }
    }
    
    @Override
    public void publishOrderCancelled(Order order) {
        if (!isEnabled()) {
            log.debug("Outbox strategy disabled, skipping order cancelled event");
            return;
        }
        
        try {
            String payload = objectMapper.writeValueAsString(order);
            OutboxEvent event = OutboxEvent.builder()
                .aggregateId(order.getId().toString())
                .eventType("ORDER_CANCELLED")
                .payload(payload)
                .partitionKey(order.getId().toString())
                .build();
            
            outboxEventRepository.save(event);
            log.info("Published ORDER_CANCELLED event for order: {}", order.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order for cancellation event publishing", e);
            throw new RuntimeException("Failed to publish order cancelled event", e);
        }
    }
    
    @Override
    public boolean isEnabled() {
        return "outbox".equalsIgnoreCase(eventPublishingStrategy);
    }
    
    // Inner class for order status update event
    private record OrderStatusUpdateEvent(
        String orderId,
        String newStatus,
        String previousStatus,
        java.time.LocalDateTime updatedAt
    ) {}
}
