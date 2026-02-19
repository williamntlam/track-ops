package com.trackops.server.adapters.input.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.adapters.input.web.dto.OrderResponse;
import com.trackops.server.adapters.output.cache.RedisOrderCacheAdapter;
import com.trackops.server.adapters.output.cache.RedisOrderStatusCacheAdapter;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.model.enums.OrderStatus;
import com.trackops.server.ports.output.cache.OrderCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Consumer for Debezium CDC events that handles Redis cache warming.
 * This consumer processes database change events captured by Debezium
 * and proactively updates Redis cache entries with fresh data.
 * 
 * Only active when app.event-publishing.strategy=debezium
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.event-publishing.strategy", havingValue = "debezium")
@RequiredArgsConstructor
public class DebeziumRedisCacheWarmer {

    private final ObjectMapper objectMapper;
    private final RedisOrderCacheAdapter orderCacheAdapter;
    private final RedisOrderStatusCacheAdapter orderStatusCacheAdapter;
    private final OrderCachePort orderCachePort;
    private final DebeziumOrderPayloadMapper payloadMapper;
    
    @Value("${app.event-publishing.strategy:outbox}")
    private String eventPublishingStrategy;
    
    @Value("${app.cache.ttl.order:3600}")
    private long orderCacheTtlSeconds;
    
    @Value("${app.cache.ttl.status:1800}")
    private long statusCacheTtlSeconds;
    
    /**
     * Consume Debezium events from the orders table for cache warming
     */
    @KafkaListener(
        topics = "trackops_orders.public.orders",
        groupId = "debezium-cache-warmer",
        containerFactory = "kafkaListenerContainerFactory",
        errorHandler = "debeziumErrorHandler"
    )
    public void handleOrderChangeForCacheWarming(
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            String payload) {
        
        // Only process if Debezium strategy is enabled
        if (!"debezium".equalsIgnoreCase(eventPublishingStrategy)) {
            log.debug("Debezium strategy disabled, ignoring Debezium cache warming event");
            return;
        }
        
        try {
            JsonNode event = objectMapper.readTree(payload);
            String operation = event.get("payload").get("op").asText();
            
            log.debug("Received Debezium cache warming event: operation={}, topic={}, partition={}, offset={}", 
                     operation, topic, partition, offset);
            
            switch (operation) {
                case "c": // Create
                    handleOrderCreatedForCacheWarming(event);
                    break;
                case "u": // Update
                    handleOrderUpdatedForCacheWarming(event);
                    break;
                case "d": // Delete
                    handleOrderDeletedForCacheWarming(event);
                    break;
                default:
                    log.warn("Unknown Debezium operation for cache warming: {}", operation);
            }
            
        } catch (Exception e) {
            log.error("Error processing Debezium cache warming event from topic: {}, partition: {}, offset: {}", 
                     topic, partition, offset, e);
        }
    }
    
    /**
     * Handle order created events for cache warming.
     * Populates Redis with full order + response (near real-time view).
     */
    private void handleOrderCreatedForCacheWarming(JsonNode event) {
        try {
            JsonNode after = event.get("payload").get("after");
            String orderIdStr = after.get("id").asText();
            String status = after.get("status").asText();
            UUID orderId = UUID.fromString(orderIdStr);

            log.info("Order created via Debezium - warming cache: orderId={}, status={}", orderId, status);

            warmOrderStatusCache(orderId, status);
            warmFullOrderAndResponse(after);

            log.info("Successfully warmed caches for new order: {}", orderId);
        } catch (Exception e) {
            log.error("Error handling order created event for cache warming", e);
        }
    }
    
    /**
     * Handle order updated events for cache warming.
     * Updates Redis with full order + response (near real-time view).
     */
    private void handleOrderUpdatedForCacheWarming(JsonNode event) {
        try {
            JsonNode before = event.get("payload").get("before");
            JsonNode after = event.get("payload").get("after");
            String orderIdStr = after.get("id").asText();
            String newStatus = after.get("status").asText();
            String previousStatus = before.get("status").asText();
            UUID orderId = UUID.fromString(orderIdStr);

            log.info("Order updated via Debezium - warming cache: orderId={}, status: {} -> {}",
                    orderId, previousStatus, newStatus);

            warmOrderStatusCache(orderId, newStatus);
            warmFullOrderAndResponse(after);

            log.info("Successfully warmed caches for updated order: {}", orderId);
        } catch (Exception e) {
            log.error("Error handling order updated event for cache warming", e);
        }
    }
    
    /**
     * Handle order deleted events for cache warming
     */
    private void handleOrderDeletedForCacheWarming(JsonNode event) {
        try {
            JsonNode before = event.get("payload").get("before");
            String orderIdStr = before.get("id").asText();
            
            UUID orderId = UUID.fromString(orderIdStr);
            
            log.info("Order deleted via Debezium - removing from cache: orderId={}", orderId);
            
            // Remove the order from cache since it's deleted
            orderCacheAdapter.removeOrder(orderId);
            orderCacheAdapter.removeOrderResponse(orderId);
            orderStatusCacheAdapter.removeOrderStatus(orderId);
            
            log.info("Successfully removed deleted order from cache: {}", orderId);
            
        } catch (Exception e) {
            log.error("Error handling order deleted event for cache warming", e);
        }
    }
    
    /**
     * Warm the order status cache with fresh data.
     */
    private void warmOrderStatusCache(UUID orderId, String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            Duration ttl = Duration.ofSeconds(statusCacheTtlSeconds);
            orderStatusCacheAdapter.cacheOrderStatus(orderId, orderStatus, ttl);
            log.debug("Warmed order status cache for order: {} with status: {}", orderId, status);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid order status: {} for order: {}", status, orderId);
        } catch (Exception e) {
            log.error("Failed to warm order status cache for order: {} with status: {}", orderId, status, e);
        }
    }

    /**
     * Populate Redis with full order entity and order response from Debezium "after" payload.
     * Keeps cache as a near real-time view without read-through.
     */
    private void warmFullOrderAndResponse(JsonNode after) {
        if (after == null) return;
        try {
            Order order = payloadMapper.orderFromAfter(after);
            OrderResponse response = payloadMapper.orderResponseFromAfter(after);
            if (order != null && response != null) {
                Duration ttl = Duration.ofSeconds(orderCacheTtlSeconds);
                orderCachePort.cacheOrder(order, ttl);
                orderCachePort.cacheOrderResponse(order.getId(), response, ttl);
                log.debug("Warmed full order and response cache for order: {}", order.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to warm full order/response cache from Debezium payload: {}", e.getMessage());
        }
    }
}
