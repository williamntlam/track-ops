package com.trackops.server.adapters.input.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.adapters.output.messaging.orders.KafkaOrderEventProducer;
import com.trackops.server.application.services.dlq.DlqOrderService;
import com.trackops.server.domain.events.orders.OrderCancelledEvent;
import com.trackops.server.domain.events.orders.OrderCreatedEvent;
import com.trackops.server.domain.model.OperationResult;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumer for Debezium CDC events from the orders table.
 * Publishes to downstream (Kafka) with a circuit breaker: when the circuit is OPEN,
 * messages are diverted to the DLQ (Pending Retry) in Postgres instead of calling Kafka.
 * Only active when app.event-publishing.strategy=debezium
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.event-publishing.strategy", havingValue = "debezium")
@RequiredArgsConstructor
public class DebeziumOrderEventConsumer {

    private final ObjectMapper objectMapper;
    private final KafkaOrderEventProducer kafkaOrderEventProducer;
    @Qualifier("downstreamKafkaCircuitBreaker")
    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;
    private final DlqOrderService dlqOrderService;

    @Value("${app.event-publishing.strategy:outbox}")
    private String eventPublishingStrategy;
    
    /**
     * Consume Debezium events from the orders table
     */
    @KafkaListener(
        id = "debezium-order-event-consumer",
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
                    handleOrderCreated(event, topic, payload);
                    break;
                case "u": // Update
                    handleOrderUpdated(event, topic, payload);
                    break;
                case "d": // Delete
                    handleOrderDeleted(event, topic, payload);
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
     * Handle order created events from Debezium. When circuit is OPEN, diverts to DLQ (Pending Retry).
     */
    private void handleOrderCreated(JsonNode event, String topic, String rawPayload) {
        try {
            JsonNode after = event.get("payload").get("after");
            String orderIdStr = after.get("id").asText();
            String status = after.get("status").asText();
            String createdBy = after.has("created_by") ? after.get("created_by").asText() : "system";

            UUID orderId = UUID.fromString(orderIdStr);

            log.info("Order created via Debezium: orderId={}, status={}, createdBy={}", orderId, status, createdBy);

            OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(orderId, createdBy);

            try {
                circuitBreaker.executeSupplier(() -> {
                    OperationResult r = kafkaOrderEventProducer.publishOrderCreated(orderCreatedEvent);
                    if (!r.isSuccess()) {
                        throw new RuntimeException(r.getErrorMessage());
                    }
                    return r;
                });
                log.info("Successfully published ORDER_CREATED event for order: {}", orderId);
            } catch (CallNotPermittedException e) {
                log.warn("Circuit is OPEN. Diverting ORDER_CREATED to DLQ (Pending Retry). orderId={}", orderId);
                dlqOrderService.saveFailedOrderEvent(topic, rawPayload, "ORDER_CREATED_circuit_open", e);
            }
        } catch (Exception e) {
            log.error("Error handling order created event", e);
        }
    }
    
    /**
     * Handle order updated events from Debezium. When circuit is OPEN, diverts to DLQ (Pending Retry).
     */
    private void handleOrderUpdated(JsonNode event, String topic, String rawPayload) {
        try {
            JsonNode before = event.get("payload").get("before");
            JsonNode after = event.get("payload").get("after");

            String orderIdStr = after.get("id").asText();
            String newStatus = after.get("status").asText();
            String previousStatus = before.get("status").asText();

            UUID orderId = UUID.fromString(orderIdStr);

            log.info("Order updated via Debezium: orderId={}, status: {} -> {}",
                    orderId, previousStatus, newStatus);

            if ("CANCELLED".equalsIgnoreCase(newStatus) && !"CANCELLED".equalsIgnoreCase(previousStatus)) {
                String cancelledBy = after.has("updated_by") ? after.get("updated_by").asText() : "system";
                String cancellationReason = after.has("cancellation_reason") ?
                        after.get("cancellation_reason").asText() : "Order cancelled via Debezium";

                log.info("Order cancelled via Debezium: orderId={}, cancelledBy={}, reason={}",
                        orderId, cancelledBy, cancellationReason);

                OrderCancelledEvent orderCancelledEvent = new OrderCancelledEvent(orderId, cancelledBy, cancellationReason);

                try {
                    circuitBreaker.executeSupplier(() -> {
                        OperationResult r = kafkaOrderEventProducer.publishOrderCancelled(orderCancelledEvent);
                        if (!r.isSuccess()) {
                            throw new RuntimeException(r.getErrorMessage());
                        }
                        return r;
                    });
                    log.info("Successfully published ORDER_CANCELLED event for order: {}", orderId);
                } catch (CallNotPermittedException e) {
                    log.warn("Circuit is OPEN. Diverting ORDER_CANCELLED to DLQ (Pending Retry). orderId={}", orderId);
                    dlqOrderService.saveFailedOrderEvent(topic, rawPayload, "ORDER_CANCELLED_circuit_open", e);
                }
            }
        } catch (Exception e) {
            log.error("Error handling order updated event", e);
        }
    }
    
    /**
     * Handle order deleted events from Debezium. When circuit is OPEN, diverts to DLQ (Pending Retry).
     */
    private void handleOrderDeleted(JsonNode event, String topic, String rawPayload) {
        try {
            JsonNode before = event.get("payload").get("before");
            String orderIdStr = before.get("id").asText();

            UUID orderId = UUID.fromString(orderIdStr);

            log.info("Order deleted via Debezium: orderId={}", orderId);

            String cancelledBy = before.has("updated_by") ? before.get("updated_by").asText() : "system";
            String cancellationReason = "Order deleted via Debezium";

            log.info("Order deleted - treating as cancellation: orderId={}, cancelledBy={}",
                    orderId, cancelledBy);

            OrderCancelledEvent orderCancelledEvent = new OrderCancelledEvent(orderId, cancelledBy, cancellationReason);

            try {
                circuitBreaker.executeSupplier(() -> {
                    OperationResult r = kafkaOrderEventProducer.publishOrderCancelled(orderCancelledEvent);
                    if (!r.isSuccess()) {
                        throw new RuntimeException(r.getErrorMessage());
                    }
                    return r;
                });
                log.info("Successfully published ORDER_CANCELLED event for deleted order: {}", orderId);
            } catch (CallNotPermittedException e) {
                log.warn("Circuit is OPEN. Diverting ORDER_CANCELLED to DLQ (Pending Retry). orderId={}", orderId);
                dlqOrderService.saveFailedOrderEvent(topic, rawPayload, "ORDER_CANCELLED_circuit_open", e);
            }
        } catch (Exception e) {
            log.error("Error handling order deleted event", e);
        }
    }
}
