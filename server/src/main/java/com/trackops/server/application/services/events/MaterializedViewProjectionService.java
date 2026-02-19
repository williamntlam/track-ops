package com.trackops.server.application.services.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.adapters.input.web.dto.OrderResponse;
import com.trackops.server.adapters.input.web.dto.mappers.OrderMapper;
import com.trackops.server.domain.model.eventstore.OrderEvent;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.model.enums.OrderStatus;
import com.trackops.server.ports.output.cache.OrderMaterializedViewPort;
import com.trackops.server.ports.output.persistence.eventstore.OrderEventStore;
import com.trackops.server.ports.output.persistence.orders.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Keeps the materialized view in sync with the event store: projects order events into
 * OrderResponse and supports read-through (rebuild from event store when view is missing).
 * Use {@link #getViewUpToDate(UUID)} for reads so the view is always accurate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterializedViewProjectionService {

    private static final Duration VIEW_TTL = Duration.ofHours(24);

    private final OrderEventStore orderEventStore;
    private final OrderMaterializedViewPort materializedViewPort;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    /**
     * Get the materialized view for an order. If missing, rebuilds from the event store
     * and writes to Redis so reads are always up to date with the event store.
     */
    public Optional<OrderResponse> getViewUpToDate(UUID orderId) {
        Optional<OrderResponse> cached = materializedViewPort.getView(orderId);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<OrderResponse> rebuilt = rebuildFromEventStore(orderId);
        rebuilt.ifPresent(view -> materializedViewPort.putView(orderId, view, VIEW_TTL));
        return rebuilt;
    }

    /**
     * Replay all events for an order and return the projected view. Does not write to Redis.
     */
    public Optional<OrderResponse> rebuildFromEventStore(UUID orderId) {
        List<OrderEvent> events = orderEventStore.findByOrderIdOrderBySequenceNumberAsc(orderId);
        if (events.isEmpty()) {
            log.debug("No events for order {}, view cannot be built from event store", orderId);
            return Optional.empty();
        }
        OrderResponse view = null;
        for (OrderEvent event : events) {
            Optional<OrderResponse> currentOpt = view != null ? Optional.of(view) : Optional.empty();
            Optional<OrderResponse> next = applyEvent(event, currentOpt, orderId);
            if (next.isEmpty()) continue;
            view = next.get();
        }
        return Optional.ofNullable(view);
    }

    /**
     * Apply a single event to the current view. Used by the listener and by replay.
     */
    public Optional<OrderResponse> applyEvent(OrderEvent event, Optional<OrderResponse> currentOpt, UUID orderId) {
        String eventType = event.getEventType();
        String payloadJson = event.getPayload();
        try {
            return switch (eventType) {
                case "ORDER_CREATED" -> {
                    Order order = objectMapper.readValue(payloadJson, Order.class);
                    OrderResponse view = orderMapper.orderToOrderResponse(order);
                    yield view != null ? Optional.of(view) : Optional.empty();
                }
                case "ORDER_CANCELLED" -> {
                    Order order = objectMapper.readValue(payloadJson, Order.class);
                    OrderResponse view = orderMapper.orderToOrderResponse(order);
                    yield view != null ? Optional.of(view) : Optional.empty();
                }
                case "ORDER_STATUS_UPDATED" -> {
                    JsonNode payload = objectMapper.readTree(payloadJson);
                    String newStatusStr = payload.has("newStatus") ? payload.get("newStatus").asText() : null;
                    String updatedAtStr = payload.has("updatedAt") ? payload.get("updatedAt").asText() : null;
                    OrderResponse current = currentOpt.orElseGet(() -> loadFromDb(orderId));
                    if (current == null) {
                        log.warn("No current view for ORDER_STATUS_UPDATED: {}", orderId);
                        yield Optional.empty();
                    }
                    OrderStatus newStatus = newStatusStr != null ? OrderStatus.valueOf(newStatusStr) : current.getStatus();
                    Instant updatedAt = updatedAtStr != null ? Instant.parse(updatedAtStr) : Instant.now();
                    yield Optional.of(OrderResponse.withStatusAndUpdatedAt(current, newStatus, updatedAt));
                }
                default -> {
                    log.debug("Ignoring event type for view: {}", eventType);
                    yield currentOpt;
                }
            };
        } catch (Exception e) {
            log.error("Failed to apply event {} for order {}: {}", eventType, orderId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private OrderResponse loadFromDb(UUID orderId) {
        return orderRepository.findById(orderId)
                .map(orderMapper::orderToOrderResponse)
                .orElse(null);
    }

    public Duration getViewTtl() {
        return VIEW_TTL;
    }
}
