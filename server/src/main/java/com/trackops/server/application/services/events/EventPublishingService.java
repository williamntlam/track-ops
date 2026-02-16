package com.trackops.server.application.services.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.domain.model.eventstore.OrderEvent;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.ports.output.persistence.eventstore.OrderEventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service that delegates event publishing to the appropriate strategy.
 * Automatically selects the enabled strategy based on configuration.
 * Also appends to the order event store for event sourcing and auditability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublishingService {

    private static final int PAYLOAD_SCHEMA_VERSION = 1;

    private final List<EventPublishingStrategy> strategies;
    private final OrderEventStore orderEventStore;
    private final ObjectMapper objectMapper;

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
        appendToEventStore(order.getId(), "ORDER_CREATED", order);
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
        appendStatusUpdatedToEventStore(order, previousStatus);
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
        appendToEventStore(order.getId(), "ORDER_CANCELLED", order);
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

    private void appendToEventStore(UUID orderId, String eventType, Order order) {
        try {
            String payloadJson = objectMapper.writeValueAsString(order);
            OrderEvent appended = orderEventStore.append(orderId, eventType, payloadJson, PAYLOAD_SCHEMA_VERSION);
            log.debug("Appended {} to event store for order {} (sequence {})", eventType, orderId, appended.getSequenceNumber());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order for event store: orderId={}", orderId, e);
        } catch (Exception e) {
            log.error("Failed to append {} to event store: orderId={}", eventType, orderId, e);
        }
    }

    private void appendStatusUpdatedToEventStore(Order order, String previousStatus) {
        try {
            String payloadJson = objectMapper.writeValueAsString(Map.of(
                "orderId", order.getId().toString(),
                "newStatus", order.getStatus().toString(),
                "previousStatus", previousStatus,
                "updatedAt", order.getUpdatedAt().toString()
            ));
            orderEventStore.append(order.getId(), "ORDER_STATUS_UPDATED", payloadJson, PAYLOAD_SCHEMA_VERSION);
            log.debug("Appended ORDER_STATUS_UPDATED to event store for order {}", order.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize status update for event store: orderId={}", order.getId(), e);
        } catch (Exception e) {
            log.error("Failed to append ORDER_STATUS_UPDATED to event store: orderId={}", order.getId(), e);
        }
    }
}
