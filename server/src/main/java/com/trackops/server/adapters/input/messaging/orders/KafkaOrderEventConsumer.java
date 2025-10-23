package com.trackops.server.adapters.input.messaging.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.ports.input.events.OrderEventProcessorPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import com.trackops.server.domain.events.orders.OrderCreatedEvent;
import com.trackops.server.domain.events.orders.OrderStatusUpdatedEvent;
import com.trackops.server.domain.events.orders.OrderDeliveredEvent;
import com.trackops.server.domain.events.orders.OrderCancelledEvent;
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

    // Your @KafkaListener methods will go here
    // Step 2: Add ORDER_CREATED listener

    @KafkaListener(
        topics = "ORDER_CREATED",
        groupId = "trackops-orders",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreated(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {

        try {
            String message = record.value();
            UUID orderId = UUID.fromString(record.key());
            String topic = record.topic();
            
            log.info("Received ORDER_CREATED event for order: {} from topic: {}", orderId, topic);
            log.debug("Message content: {}", message);

            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
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

    // Step 3: Add ORDER_STATUS_UPDATED listener  
    @KafkaListener(
        topics = "ORDER_STATUS_UPDATED",
        groupId = "trackops-orders",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderStatusUpdated(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            String message = record.value();
            UUID orderId = UUID.fromString(record.key());
            String topic = record.topic();
            
            log.info("Received ORDER_STATUS_UPDATED event for order: {} from topic: {}", orderId, topic);
            log.debug("Message content: {}", message);
            
            OrderStatusUpdatedEvent event = objectMapper.readValue(message, OrderStatusUpdatedEvent.class);
            orderEventProcessor.processOrderEvent(event);

            log.info("Successfully processed ORDER_STATUS_UPDATED event for order: {}", orderId);
            acknowledgment.acknowledge();
            
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize ORDER_STATUS_UPDATED event for order: {} - Invalid JSON format", record.key(), e);
            acknowledgment.acknowledge(); // Acknowledge malformed messages to avoid infinite retry
        } catch (Exception e) {
            log.error("Failed to process ORDER_STATUS_UPDATED event for order: {}", record.key(), e);
            // Don't acknowledge - let Kafka retry the message
            throw e; // Re-throw to trigger retry mechanism
        }
    }

    @KafkaListener(
        topics = "ORDER_DELIVERED",
        groupId = "trackops-orders",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderDelivered(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        
        try {
            String message = record.value();
            UUID orderId = UUID.fromString(record.key());
            String topic = record.topic();
            
            log.info("Received ORDER_DELIVERED event for order: {} from topic: {}", orderId, topic);
            log.debug("Message content: {}", message);
            
            OrderDeliveredEvent event = objectMapper.readValue(message, OrderDeliveredEvent.class);
            orderEventProcessor.processOrderEvent(event);

            log.info("Successfully processed ORDER_DELIVERED event for order: {}", orderId);
            acknowledgment.acknowledge();
            
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize ORDER_DELIVERED event for order: {} - Invalid JSON format", record.key(), e);
            acknowledgment.acknowledge(); // Acknowledge malformed messages to avoid infinite retry
        } catch (Exception e) {
            log.error("Failed to process ORDER_DELIVERED event for order: {}", record.key(), e);
            // Don't acknowledge - let Kafka retry the message
            throw e; // Re-throw to trigger retry mechanism
        }
    }

    @KafkaListener(
        topics = "ORDER_CANCELLED",
        groupId = "trackops-orders",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCancelled(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        
        try {
            String message = record.value();
            UUID orderId = UUID.fromString(record.key());
            String topic = record.topic();
            
            log.info("Received ORDER_CANCELLED event for order: {} from topic: {}", orderId, topic);
            log.debug("Message content: {}", message);
            
            OrderCancelledEvent event = objectMapper.readValue(message, OrderCancelledEvent.class);
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