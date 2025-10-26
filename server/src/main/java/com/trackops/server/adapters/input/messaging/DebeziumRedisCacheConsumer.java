package com.trackops.server.adapters.input.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.adapters.output.cache.RedisOrderCacheAdapter;
import com.trackops.server.adapters.output.cache.RedisOrderStatusCacheAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumer for Debezium CDC events that handles Redis cache invalidation.
 * This consumer processes database change events captured by Debezium
 * and invalidates/updates related Redis cache entries to keep them fresh.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DebeziumRedisCacheConsumer {
    
    private final ObjectMapper objectMapper;
    private final RedisOrderCacheAdapter orderCacheAdapter;
    private final RedisOrderStatusCacheAdapter orderStatusCacheAdapter;
    
    @Value("${app.event-publishing.strategy:outbox}")
    private String eventPublishingStrategy;
    
    /**
     * Consume Debezium events from the orders table for cache invalidation
     */
    @KafkaListener(
        topics = "trackops_orders.public.orders",
        groupId = "debezium-cache-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderChangeForCache(
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            String payload) {
        
        // Only process if Debezium strategy is enabled
        if (!"debezium".equalsIgnoreCase(eventPublishingStrategy)) {
            log.debug("Debezium strategy disabled, ignoring Debezium cache event");
            return;
        }
        
        try {
            JsonNode event = objectMapper.readTree(payload);
            String operation = event.get("payload").get("op").asText();
            
            log.debug("Received Debezium cache event: operation={}, topic={}, partition={}, offset={}", 
                     operation, topic, partition, offset);
            
            switch (operation) {
                case "c": // Create
                    handleOrderCreatedForCache(event);
                    break;
                case "u": // Update
                    handleOrderUpdatedForCache(event);
                    break;
                case "d": // Delete
                    handleOrderDeletedForCache(event);
                    break;
                default:
                    log.warn("Unknown Debezium operation for cache: {}", operation);
            }
            
        } catch (Exception e) {
            log.error("Error processing Debezium cache event from topic: {}, partition: {}, offset: {}", 
                     topic, partition, offset, e);
        }
    }
    
    /**
     * Handle order created events for cache invalidation
     */
    private void handleOrderCreatedForCache(JsonNode event) {
        try {
            JsonNode after = event.get("payload").get("after");
            String orderIdStr = after.get("id").asText();
            String status = after.get("status").asText();
            String customerId = after.has("customer_id") ? after.get("customer_id").asText() : null;
            
            UUID orderId = UUID.fromString(orderIdStr);
            
            log.info("Order created via Debezium - invalidating cache: orderId={}, status={}", orderId, status);
            
            // Invalidate related caches since a new order was created
            invalidateOrderRelatedCaches(orderId, customerId, status);
            
            log.info("Successfully invalidated caches for new order: {}", orderId);
            
        } catch (Exception e) {
            log.error("Error handling order created event for cache", e);
        }
    }
    
    /**
     * Handle order updated events for cache invalidation
     */
    private void handleOrderUpdatedForCache(JsonNode event) {
        try {
            JsonNode before = event.get("payload").get("before");
            JsonNode after = event.get("payload").get("after");
            
            String orderIdStr = after.get("id").asText();
            String newStatus = after.get("status").asText();
            String previousStatus = before.get("status").asText();
            String customerId = after.has("customer_id") ? after.get("customer_id").asText() : null;
            
            UUID orderId = UUID.fromString(orderIdStr);
            
            log.info("Order updated via Debezium - invalidating cache: orderId={}, status: {} -> {}", 
                    orderId, previousStatus, newStatus);
            
            // Always invalidate order-specific caches
            invalidateOrderSpecificCaches(orderId);
            
            // If status changed, invalidate status-related caches
            if (!newStatus.equals(previousStatus)) {
                invalidateStatusRelatedCaches(previousStatus, newStatus);
            }
            
            // If customer changed, invalidate customer-related caches
            String previousCustomerId = before.has("customer_id") ? before.get("customer_id").asText() : null;
            if (customerId != null && !customerId.equals(previousCustomerId)) {
                if (previousCustomerId != null) {
                    invalidateCustomerRelatedCaches(UUID.fromString(previousCustomerId));
                }
                if (customerId != null) {
                    invalidateCustomerRelatedCaches(UUID.fromString(customerId));
                }
            }
            
            log.info("Successfully invalidated caches for updated order: {}", orderId);
            
        } catch (Exception e) {
            log.error("Error handling order updated event for cache", e);
        }
    }
    
    /**
     * Handle order deleted events for cache invalidation
     */
    private void handleOrderDeletedForCache(JsonNode event) {
        try {
            JsonNode before = event.get("payload").get("before");
            String orderIdStr = before.get("id").asText();
            String status = before.get("status").asText();
            String customerId = before.has("customer_id") ? before.get("customer_id").asText() : null;
            
            UUID orderId = UUID.fromString(orderIdStr);
            
            log.info("Order deleted via Debezium - invalidating cache: orderId={}, status={}", orderId, status);
            
            // Invalidate all related caches since order was deleted
            invalidateOrderRelatedCaches(orderId, customerId, status);
            
            log.info("Successfully invalidated caches for deleted order: {}", orderId);
            
        } catch (Exception e) {
            log.error("Error handling order deleted event for cache", e);
        }
    }
    
    /**
     * Invalidate all caches related to an order
     */
    private void invalidateOrderRelatedCaches(UUID orderId, String customerId, String status) {
        // Invalidate order-specific caches
        invalidateOrderSpecificCaches(orderId);
        
        // Invalidate status-related caches
        if (status != null) {
            invalidateStatusRelatedCaches(status, null);
        }
        
        // Invalidate customer-related caches
        if (customerId != null) {
            try {
                invalidateCustomerRelatedCaches(UUID.fromString(customerId));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid customer ID format: {}", customerId);
            }
        }
    }
    
    /**
     * Invalidate order-specific caches
     */
    private void invalidateOrderSpecificCaches(UUID orderId) {
        try {
            // Remove order entity cache
            orderCacheAdapter.removeOrder(orderId);
            
            // Remove order response cache
            orderCacheAdapter.removeOrderResponse(orderId);
            
            // Remove order status cache
            orderStatusCacheAdapter.removeOrderStatus(orderId);
            
            log.debug("Invalidated order-specific caches for order: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to invalidate order-specific caches for order: {}", orderId, e);
        }
    }
    
    /**
     * Invalidate status-related caches
     */
    private void invalidateStatusRelatedCaches(String previousStatus, String newStatus) {
        try {
            // Remove previous status cache
            if (previousStatus != null) {
                orderCacheAdapter.removeOrdersByStatus(previousStatus);
                log.debug("Invalidated cache for previous status: {}", previousStatus);
            }
            
            // Remove new status cache
            if (newStatus != null) {
                orderCacheAdapter.removeOrdersByStatus(newStatus);
                log.debug("Invalidated cache for new status: {}", newStatus);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate status-related caches: {} -> {}", previousStatus, newStatus, e);
        }
    }
    
    /**
     * Invalidate customer-related caches
     */
    private void invalidateCustomerRelatedCaches(UUID customerId) {
        try {
            orderCacheAdapter.removeOrdersByCustomer(customerId);
            log.debug("Invalidated customer-related caches for customer: {}", customerId);
        } catch (Exception e) {
            log.error("Failed to invalidate customer-related caches for customer: {}", customerId, e);
        }
    }
}
