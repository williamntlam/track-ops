package com.trackops.server.application.services.events;

import com.trackops.server.domain.model.orders.Order;

/**
 * Strategy interface for publishing events.
 * Allows switching between Outbox Pattern and Debezium CDC.
 */
public interface EventPublishingStrategy {
    
    /**
     * Publish an order created event
     * @param order the order that was created
     */
    void publishOrderCreated(Order order);
    
    /**
     * Publish an order status updated event
     * @param order the order that was updated
     * @param previousStatus the previous status
     */
    void publishOrderStatusUpdated(Order order, String previousStatus);
    
    /**
     * Publish an order cancelled event
     * @param order the order that was cancelled
     */
    void publishOrderCancelled(Order order);
    
    /**
     * Check if this strategy is enabled
     * @return true if this strategy should be used
     */
    boolean isEnabled();
}
