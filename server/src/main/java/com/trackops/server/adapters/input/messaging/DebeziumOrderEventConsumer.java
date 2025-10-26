package com.trackops.server.adapters.input.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.adapters.output.messaging.orders.KafkaOrderEventProducer;
import com.trackops.server.domain.events.orders.OrderCreatedEvent;
import com.trackops.server.domain.events.orders.OrderCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumer for Debezium CDC events from the orders table.
 * This consumer processes database change events captured by Debezium
 * and converts them into business events for other services.
 * 
 * Only active when app.event-publishing.strategy=debezium
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.event-publishing.strategy", havingValue = "debezium")
@RequiredArgsConstructor
public class DebeziumOrderEventConsumer {
    
    private final ObjectMapper objectMapper;
    private final KafkaOrderEventProducer kafkaOrderEventProducer;
    
    @Value("${app.event-publishing.strategy:outbox}")
    private String eventPublishingStrategy;
    
    /**
     * Consume Debezium events from the orders table
     */
    @KafkaListener(
        topics = "trackops_orders.public.orders",
        groupId = "debezium-order-consumer",
        containerFactory = "kafkaListenerContainerFactory",
        errorHandler = "debeziumErrorHandler"
    )
    public void handleOrderChange(
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            String payload) {
        
        // Only process if Debezium strategy is enabled
        if (!"debezium".equalsIgnoreCase(eventPublishingStrategy)) {
            log.debug("Debezium strategy disabled, ignoring Debezium event");
            return;
        }
        
        try {
            JsonNode event = objectMapper.readTree(payload);
            String operation = event.get("payload").get("op").asText();
            
            log.debug("Received Debezium event: operation={}, topic={}, partition={}, offset={}", 
                     operation, topic, partition, offset);
            
            switch (operation) {
                case "c": // Create
                    handleOrderCreated(event);
                    break;
                case "u": // Update
                    handleOrderUpdated(event);
                    break;
                case "d": // Delete
                    handleOrderDeleted(event);
                    break;
                default:
                    log.warn("Unknown Debezium operation: {}", operation);
            }
            
        } catch (Exception e) {
            log.error("Error processing Debezium event from topic: {}, partition: {}, offset: {}", 
                     topic, partition, offset, e);
        }
    }
    
    /**
     * Handle order created events from Debezium
     */
    private void handleOrderCreated(JsonNode event) {
        try {
            JsonNode after = event.get("payload").get("after");
            String orderIdStr = after.get("id").asText();
            String status = after.get("status").asText();
            String createdBy = after.has("created_by") ? after.get("created_by").asText() : "system";
            
            UUID orderId = UUID.fromString(orderIdStr);
            
            log.info("Order created via Debezium: orderId={}, status={}, createdBy={}", orderId, status, createdBy);
            
            // Create and publish ORDER_CREATED event for inventory service
            OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(orderId, createdBy);
            kafkaOrderEventProducer.publishOrderCreated(orderCreatedEvent);
            
            log.info("Successfully published ORDER_CREATED event for order: {}", orderId);
            
        } catch (Exception e) {
            log.error("Error handling order created event", e);
        }
    }
    
    /**
     * Handle order updated events from Debezium
     */
    private void handleOrderUpdated(JsonNode event) {
        try {
            JsonNode before = event.get("payload").get("before");
            JsonNode after = event.get("payload").get("after");
            
            String orderIdStr = after.get("id").asText();
            String newStatus = after.get("status").asText();
            String previousStatus = before.get("status").asText();
            
            UUID orderId = UUID.fromString(orderIdStr);
            
            log.info("Order updated via Debezium: orderId={}, status: {} -> {}", 
                    orderId, previousStatus, newStatus);
            
            // Check if order was cancelled
            if ("CANCELLED".equalsIgnoreCase(newStatus) && !"CANCELLED".equalsIgnoreCase(previousStatus)) {
                String cancelledBy = after.has("updated_by") ? after.get("updated_by").asText() : "system";
                String cancellationReason = after.has("cancellation_reason") ? 
                    after.get("cancellation_reason").asText() : "Order cancelled via Debezium";
                
                log.info("Order cancelled via Debezium: orderId={}, cancelledBy={}, reason={}", 
                        orderId, cancelledBy, cancellationReason);
                
                // Create and publish ORDER_CANCELLED event for inventory service
                OrderCancelledEvent orderCancelledEvent = new OrderCancelledEvent(orderId, cancelledBy, cancellationReason);
                kafkaOrderEventProducer.publishOrderCancelled(orderCancelledEvent);
                
                log.info("Successfully published ORDER_CANCELLED event for order: {}", orderId);
            }
            
        } catch (Exception e) {
            log.error("Error handling order updated event", e);
        }
    }
    
    /**
     * Handle order deleted events from Debezium
     */
    private void handleOrderDeleted(JsonNode event) {
        try {
            JsonNode before = event.get("payload").get("before");
            String orderIdStr = before.get("id").asText();
            
            UUID orderId = UUID.fromString(orderIdStr);
            
            log.info("Order deleted via Debezium: orderId={}", orderId);
            
            // Treat deletion as cancellation for inventory purposes
            String cancelledBy = before.has("updated_by") ? before.get("updated_by").asText() : "system";
            String cancellationReason = "Order deleted via Debezium";
            
            log.info("Order deleted - treating as cancellation: orderId={}, cancelledBy={}", 
                    orderId, cancelledBy);
            
            // Create and publish ORDER_CANCELLED event for inventory service
            OrderCancelledEvent orderCancelledEvent = new OrderCancelledEvent(orderId, cancelledBy, cancellationReason);
            kafkaOrderEventProducer.publishOrderCancelled(orderCancelledEvent);
            
            log.info("Successfully published ORDER_CANCELLED event for deleted order: {}", orderId);
            
        } catch (Exception e) {
            log.error("Error handling order deleted event", e);
        }
    }
}
