package com.trackops.server.adapters.input.messaging.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.ports.input.events.OrderEventProcessorPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
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
    public void handleOrderCreated(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) UUID orderId,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        Acknowledgment acknowledgment) {

            try {

                log.info("Received ORDER_CREATED event for order: {} from {}", orderId, topic);
                log.debug("Message content: {}", message);

                log.info("Successfully processed ORDER_CREATED event for order {}", orderId);

                OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

                orderEventProcessor.processOrderEvent(event);

                log.info("Successfully processed ORDER_CREATED event for order {}", orderId);
                acknowledgment.acknowledge();

            } 

            catch(Exception e) {

                log.error("Failed to process ORDER_CREATED event for order: {}", orderId, e);

            }

    }

    // Step 3: Add ORDER_STATUS_UPDATED listener  
    @KafkaListener(
        topics = "ORDER_STATUS_UPDATED",
        groupId = "trackops-orders",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderStatusUpdated(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) UUID orderId,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        Acknowledgment acknowledgment 
    ) {
        try {
            log.info("Received ORDER_STATUS_UPDATED event for order: {} from topic: {}", orderId, topic);
            
            log.debug("Message content: {}", message);
            
            OrderStatusUpdatedEvent event = ObjectMapper.readValue(message, OrderStatusUpdatedEvent.class);
            
            orderEventProcessor.processOrderEvent(event);

            log.info("Successfully processed ORDER_STATUS_UPDATED event for order: {}", orderId);
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process ORDER_STATUS_UPDATED event for order: {}", orderId, e);
        }
    }

    @KafkaListener(
        topics = "ORDER_DELIVERED",
        groupId = "trackops-orders",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderDelivered(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) UUID orderId,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received ORDER_DELIVERED event for order: {} from topic: {}", orderId, topic);
            
            log.debug("Message content: {}", message);
            
            
            log.info("Successfully processed ORDER_DELIVERED event for order: {}", orderId);
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process ORDER_DELIVERED event for order: {}", orderId, e);
        }
    }

    @KafkaListener(
        topics = "ORDER_CANCELLED",
        groupId = "trackops-orders",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCancelled(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) UUID orderId,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received ORDER_CANCELLED event for order: {} from topic: {}", orderId, topic);
            
            log.debug("Message content: {}", message);
            
            
            log.info("Successfully processed ORDER_CANCELLED event for order: {}", orderId);
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process ORDER_CANCELLED event for order: {}", orderId, e);
            // Don't acknowledge on error - let Kafka retry
        }
    }
}