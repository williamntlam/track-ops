package com.trackops.inventory.adapters.input.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.inventory.domain.events.OrderEvent;
import com.trackops.inventory.ports.input.events.OrderEventProcessorPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.UUID;

@Slf4j
@Component
public class KafkaOrderEventConsumer {

    private final OrderEventProcessorPort orderEventProcessor;
    private final ObjectMapper objectMapper;

    public KafkaOrderEventConsumer(OrderEventProcessorPort orderEventProcessor, ObjectMapper objectMapper) {
        this.orderEventProcessor = orderEventProcessor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = "ORDER_CREATED",
        groupId = "inventory-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreated(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            String message = record.value();
            UUID orderId = UUID.fromString(record.key());
            String topic = record.topic();
            
            log.info("Received ORDER_CREATED event for order: {} from topic: {}", orderId, topic);
            log.debug("Message content: {}", message);

            // Create a simple OrderEvent for processing
            OrderEvent event = new OrderEvent("ORDER_CREATED", orderId) {};
            orderEventProcessor.processOrderEvent(event);

            log.info("Successfully processed ORDER_CREATED event for order: {}", orderId);
            acknowledgment.acknowledge();

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize ORDER_CREATED event for order: {} - Invalid JSON format", record.key(), e);
            acknowledgment.acknowledge(); // Acknowledge malformed messages to avoid infinite retry
        } catch (Exception e) {
            log.error("Failed to process ORDER_CREATED event for order: {}", record.key(), e);
            // Don't acknowledge - let Kafka retry the message
            throw e; // Re-throw to trigger retry mechanism
        }
    }

    @KafkaListener(
        topics = "ORDER_CANCELLED",
        groupId = "inventory-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCancelled(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            String message = record.value();
            UUID orderId = UUID.fromString(record.key());
            String topic = record.topic();
            
            log.info("Received ORDER_CANCELLED event for order: {} from topic: {}", orderId, topic);
            log.debug("Message content: {}", message);

            // Create a simple OrderEvent for processing
            OrderEvent event = new OrderEvent("ORDER_CANCELLED", orderId) {};
            orderEventProcessor.processOrderEvent(event);

            log.info("Successfully processed ORDER_CANCELLED event for order: {}", orderId);
            acknowledgment.acknowledge();

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize ORDER_CANCELLED event for order: {} - Invalid JSON format", record.key(), e);
            acknowledgment.acknowledge(); // Acknowledge malformed messages to avoid infinite retry
        } catch (Exception e) {
            log.error("Failed to process ORDER_CANCELLED event for order: {}", record.key(), e);
            // Don't acknowledge - let Kafka retry the message
            throw e; // Re-throw to trigger retry mechanism
        }
    }
}
