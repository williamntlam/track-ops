package com.trackops.server.adapters.input.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.adapters.output.cache.RedisOrderCacheAdapter;
import com.trackops.server.adapters.output.cache.RedisOrderStatusCacheAdapter;
import com.trackops.server.domain.model.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DebeziumRedisCacheWarmer {
    
    private final ObjectMapper objectMapper;
    private final RedisOrderCacheAdapter orderCacheAdapter;
    private final RedisOrderStatusCacheAdapter orderStatusCacheAdapter;
    
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
        containerFactory = "kafkaListenerContainerFactory"
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
     * Handle order created events for cache warming
     */
    private void handleOrderCreatedForCacheWarming(JsonNode event) {
        try {
            JsonNode after = event.get("payload").get("after");
            String orderIdStr = after.get("id").asText();
            String status = after.get("status").asText();
            
            UUID orderId = UUID.fromString(orderIdStr);
            
            log.info("Order created via Debezium - warming cache: orderId={}, status={}", orderId, status);
            
            // Warm the order status cache with the new status
            warmOrderStatusCache(orderId, status);
            
            log.info("Successfully warmed caches for new order: {}", orderId);
            
        } catch (Exception e) {
            log.error("Error handling order created event for cache warming", e);
        }
    }
    
    /**
     * Handle order updated events for cache warming
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
            
            // Warm the order status cache with the new status
            warmOrderStatusCache(orderId, newStatus);
            
            // If this is a frequently accessed order, we could also warm the full order cache
            // This would require fetching the full order from the database
            // For now, we'll just warm the status cache which is most commonly accessed
            
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
     * Warm the order status cache with fresh data
     */
    private void warmOrderStatusCache(UUID orderId, String status) {
        try {
            // Convert string status to OrderStatus enum
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            
            // Cache the order status with TTL
            Duration ttl = Duration.ofSeconds(statusCacheTtlSeconds);
            orderStatusCacheAdapter.cacheOrderStatus(orderId, orderStatus, ttl);
            
            log.debug("Warmed order status cache for order: {} with status: {}", orderId, status);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid order status: {} for order: {}", status, orderId);
        } catch (Exception e) {
            log.error("Failed to warm order status cache for order: {} with status: {}", orderId, status, e);
        }
    }
    
    // Note: Full order cache warming could be implemented here
    // This would involve fetching the complete order from the database
    // and caching it with appropriate TTL for frequently accessed orders
}
