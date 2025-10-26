package com.trackops.server.application.services.events;

import com.trackops.server.domain.model.orders.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service that delegates event publishing to the appropriate strategy.
 * Automatically selects the enabled strategy based on configuration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublishingService {
    
    private final List<EventPublishingStrategy> strategies;
    
    /**
     * Publish an order created event using the enabled strategy
     */
    public void publishOrderCreated(Order order) {
        EventPublishingStrategy strategy = getEnabledStrategy();
        if (strategy != null) {
            strategy.publishOrderCreated(order);
        } else {
            log.warn("No event publishing strategy is enabled");
        }
    }
    
    /**
     * Publish an order status updated event using the enabled strategy
     */
    public void publishOrderStatusUpdated(Order order, String previousStatus) {
        EventPublishingStrategy strategy = getEnabledStrategy();
        if (strategy != null) {
            strategy.publishOrderStatusUpdated(order, previousStatus);
        } else {
            log.warn("No event publishing strategy is enabled");
        }
    }
    
    /**
     * Publish an order cancelled event using the enabled strategy
     */
    public void publishOrderCancelled(Order order) {
        EventPublishingStrategy strategy = getEnabledStrategy();
        if (strategy != null) {
            strategy.publishOrderCancelled(order);
        } else {
            log.warn("No event publishing strategy is enabled");
        }
    }
    
    /**
     * Get the currently enabled event publishing strategy
     */
    private EventPublishingStrategy getEnabledStrategy() {
        return strategies.stream()
            .filter(EventPublishingStrategy::isEnabled)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get the name of the currently enabled strategy
     */
    public String getEnabledStrategyName() {
        EventPublishingStrategy strategy = getEnabledStrategy();
        return strategy != null ? strategy.getClass().getSimpleName() : "None";
    }
}
