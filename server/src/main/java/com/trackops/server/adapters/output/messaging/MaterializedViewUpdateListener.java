package com.trackops.server.adapters.output.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.adapters.input.web.dto.OrderResponse;
import com.trackops.server.adapters.input.web.dto.mappers.OrderMapper;
import com.trackops.server.domain.model.eventstore.OrderEvent;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.model.enums.OrderStatus;
import com.trackops.server.ports.output.cache.OrderMaterializedViewPort;
import com.trackops.server.ports.output.persistence.orders.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Listens to order events appended to the event store and updates the Redis materialized view
 * (update-on-write). Keeps the read model in Redis so GET /orders/{id} can serve from Redis without
 * hitting PostgreSQL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaterializedViewUpdateListener {

    private static final Duration VIEW_TTL = Duration.ofHours(24);

    private final OrderMaterializedViewPort materializedViewPort;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    @EventListener
    public void onOrderEventAppended(OrderEvent event) {
        UUID orderId = event.getOrderId();
        String eventType = event.getEventType();
        String payloadJson = event.getPayload();
        try {
            Optional<OrderResponse> currentOpt = materializedViewPort.getView(orderId);
            OrderResponse viewToWrite;

            switch (eventType) {
                case "ORDER_CREATED" -> {
                    Order order = objectMapper.readValue(payloadJson, Order.class);
                    viewToWrite = orderMapper.orderToOrderResponse(order);
                    if (viewToWrite == null) {
                        log.warn("Could not map order to response for view: {}", orderId);
                        return;
                    }
                }
                case "ORDER_CANCELLED" -> {
                    Order order = objectMapper.readValue(payloadJson, Order.class);
                    viewToWrite = orderMapper.orderToOrderResponse(order);
                    if (viewToWrite == null) {
                        log.warn("Could not map order to response for view: {}", orderId);
                        return;
                    }
                }
                case "ORDER_STATUS_UPDATED" -> {
                    JsonNode payload = objectMapper.readTree(payloadJson);
                    String newStatusStr = payload.has("newStatus") ? payload.get("newStatus").asText() : null;
                    String updatedAtStr = payload.has("updatedAt") ? payload.get("updatedAt").asText() : null;
                    OrderResponse current = currentOpt.orElseGet(() -> loadFromDbAndPopulate(orderId));
                    if (current == null) {
                        log.warn("No current view for ORDER_STATUS_UPDATED: {}", orderId);
                        return;
                    }
                    OrderStatus newStatus = newStatusStr != null ? OrderStatus.valueOf(newStatusStr) : current.getStatus();
                    Instant updatedAt = updatedAtStr != null ? Instant.parse(updatedAtStr) : Instant.now();
                    viewToWrite = OrderResponse.withStatusAndUpdatedAt(current, newStatus, updatedAt);
                }
                default -> {
                    log.debug("Ignoring event type for view update: {}", eventType);
                    return;
                }
            }

            materializedViewPort.putView(orderId, viewToWrite, VIEW_TTL);
            log.debug("Updated materialized view for order {} after {}", orderId, eventType);
        } catch (Exception e) {
            log.error("Failed to update materialized view for order {} on {}: {}", orderId, eventType, e.getMessage(), e);
        }
    }

    /**
     * On cache miss in the projector: load order from PostgreSQL and populate Redis so next time we have a view.
     */
    private OrderResponse loadFromDbAndPopulate(UUID orderId) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    OrderResponse response = orderMapper.orderToOrderResponse(order);
                    if (response != null) {
                        materializedViewPort.putView(orderId, response, VIEW_TTL);
                    }
                    return response;
                })
                .orElse(null);
    }
}
