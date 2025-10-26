package com.trackops.server.adapters.input.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.application.services.events.EventPublishingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumer for Debezium CDC events from the orders table.
 * This consumer processes database change events captured by Debezium
 * and converts them into business events for other services.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DebeziumOrderEventConsumer {
    
    private final ObjectMapper objectMapper;
    private final EventPublishingService eventPublishingService;
    
    @Value("${app.event-publishing.strategy:outbox}")
    private String eventPublishingStrategy;
    
    /**
     * Consume Debezium events from the orders table
     */
    @KafkaListener(
        topics = "trackops_orders.public.orders",
        groupId = "debezium-order-consumer",
        containerFactory = "kafkaListenerContainerFactory"
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
            String orderId = after.get("id").asText();
            String status = after.get("status").asText();
            
            log.info("Order created via Debezium: orderId={}, status={}", orderId, status);
            
            // Here you could trigger additional business logic
            // For example, notify other services about the new order
            // This is where you'd integrate with your existing business logic
            
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
            
            String orderId = after.get("id").asText();
            String newStatus = after.get("status").asText();
            String previousStatus = before.get("status").asText();
            
            log.info("Order updated via Debezium: orderId={}, status: {} -> {}", 
                    orderId, previousStatus, newStatus);
            
            // Here you could trigger additional business logic
            // For example, notify other services about the status change
            
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
            String orderId = before.get("id").asText();
            
            log.info("Order deleted via Debezium: orderId={}", orderId);
            
            // Here you could trigger additional business logic
            // For example, cleanup related data or notify other services
            
        } catch (Exception e) {
            log.error("Error handling order deleted event", e);
        }
    }
}
