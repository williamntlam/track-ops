package com.trackops.server.application.services.events;

import com.trackops.server.domain.model.orders.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Debezium CDC implementation for event publishing.
 * With Debezium, events are automatically captured from database changes,
 * so this strategy does minimal work - mainly logging for debugging.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DebeziumEventPublishingStrategy implements EventPublishingStrategy {
    
    @Value("${app.event-publishing.strategy:outbox}")
    private String eventPublishingStrategy;
    
    @Override
    public void publishOrderCreated(Order order) {
        if (!isEnabled()) {
            log.debug("Debezium strategy disabled, skipping order created event");
            return;
        }
        
        // With Debezium, the database change will automatically trigger an event
        // We just log for debugging purposes
        log.info("Order created - Debezium will automatically capture this change: {}", order.getId());
    }
    
    @Override
    public void publishOrderStatusUpdated(Order order, String previousStatus) {
        if (!isEnabled()) {
            log.debug("Debezium strategy disabled, skipping order status updated event");
            return;
        }
        
        // With Debezium, the database change will automatically trigger an event
        log.info("Order status updated from {} to {} - Debezium will automatically capture this change: {}", 
                previousStatus, order.getStatus(), order.getId());
    }
    
    @Override
    public void publishOrderCancelled(Order order) {
        if (!isEnabled()) {
            log.debug("Debezium strategy disabled, skipping order cancelled event");
            return;
        }
        
        // With Debezium, the database change will automatically trigger an event
        log.info("Order cancelled - Debezium will automatically capture this change: {}", order.getId());
    }
    
    @Override
    public boolean isEnabled() {
        return "debezium".equalsIgnoreCase(eventPublishingStrategy);
    }
}
