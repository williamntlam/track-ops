package com.trackops.server.adapters.input.messaging.orders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.config.AvroEventConverter;
import com.trackops.server.ports.input.events.OrderEventProcessorPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.trackops.server.domain.events.orders.OrderCreatedEvent;
import com.trackops.server.domain.events.orders.OrderStatusUpdatedEvent;
import com.trackops.server.domain.events.orders.OrderDeliveredEvent;
import com.trackops.server.domain.events.orders.OrderCancelledEvent;
import com.trackops.server.domain.events.orders.InventoryReservedEvent;
import com.trackops.server.domain.events.orders.InventoryReservationFailedEvent;
import com.trackops.server.domain.events.orders.InventoryReleasedEvent;

import java.util.UUID;

/**
 * Consumes order events from Kafka. Processing is transactional; Kafka ACK is sent only
 * after the transaction commits, so a crash after DB write but before ACK results in
 * redelivery. Idempotency is enforced via processed_events INSERT ON CONFLICT (event_id) DO NOTHING.
 */
@Slf4j
@Component
public class KafkaOrderEventConsumer {

    private final OrderEventProcessorPort orderEventProcessor;
    private final AvroEventConverter avroEventConverter;
    private final ObjectMapper objectMapper;

    public KafkaOrderEventConsumer(OrderEventProcessorPort orderEventProcessor, AvroEventConverter avroEventConverter, ObjectMapper objectMapper) {
        this.orderEventProcessor = orderEventProcessor;
        this.avroEventConverter = avroEventConverter;
        this.objectMapper = objectMapper;
    }

    // Your @KafkaListener methods will go here
    // Step 2: Add ORDER_CREATED listener

    @KafkaListener(
        topics = "ORDER_CREATED",
        groupId = "trackops-orders",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreated(ConsumerRecord<UUID, GenericRecord> record, Acknowledgment acknowledgment) {

        try {
            GenericRecord avroRecord = record.value();
            UUID orderId = record.key();
            String topic = record.topic();
            
            log.info("Received ORDER_CREATED event for order: {} from topic: {}", orderId, topic);
            log.debug("Avro record: {}", avroRecord);

            OrderCreatedEvent event = avroEventConverter.fromAvro(avroRecord);
            orderEventProcessor.processOrderEvent(event);
            log.info("Successfully processed ORDER_CREATED event for order: {}", orderId);
            // ACK only after transactional processing has committed (processOrderEvent is @Transactional)
            acknowledgment.acknowledge();
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
    public void handleOrderStatusUpdated(ConsumerRecord<UUID, GenericRecord> record, Acknowledgment acknowledgment) {
        try {
            GenericRecord avroRecord = record.value();
            UUID orderId = record.key();
            String topic = record.topic();
            
            log.info("Received ORDER_STATUS_UPDATED event for order: {} from topic: {}", orderId, topic);
            log.debug("Avro record: {}", avroRecord);
            
            OrderStatusUpdatedEvent event = avroEventConverter.fromAvroStatusUpdated(avroRecord);
            orderEventProcessor.processOrderEvent(event);

            log.info("Successfully processed ORDER_STATUS_UPDATED event for order: {}", orderId);
            acknowledgment.acknowledge();
            
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
    public void handleOrderDelivered(ConsumerRecord<UUID, GenericRecord> record, Acknowledgment acknowledgment) {
        
        try {
            GenericRecord avroRecord = record.value();
            UUID orderId = record.key();
            String topic = record.topic();
            
            log.info("Received ORDER_DELIVERED event for order: {} from topic: {}", orderId, topic);
            log.debug("Avro record: {}", avroRecord);
            
            OrderDeliveredEvent event = avroEventConverter.fromAvroDelivered(avroRecord);
            orderEventProcessor.processOrderEvent(event);

            log.info("Successfully processed ORDER_DELIVERED event for order: {}", orderId);
            acknowledgment.acknowledge();
            
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
    public void handleOrderCancelled(ConsumerRecord<UUID, GenericRecord> record, Acknowledgment acknowledgment) {
        
        try {
            GenericRecord avroRecord = record.value();
            UUID orderId = record.key();
            String topic = record.topic();
            
            log.info("Received ORDER_CANCELLED event for order: {} from topic: {}", orderId, topic);
            log.debug("Avro record: {}", avroRecord);
            
            OrderCancelledEvent event = avroEventConverter.fromAvroCancelled(avroRecord);
            orderEventProcessor.processOrderEvent(event);
            
            log.info("Successfully processed ORDER_CANCELLED event for order: {}", orderId);
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process ORDER_CANCELLED event for order: {}", record.key(), e);
            // Don't acknowledge - let Kafka retry the message
            throw e; // Re-throw to trigger retry mechanism
        }
    }

    // Inventory Service Response Event Handlers
    @KafkaListener(
        topics = "INVENTORY_RESERVED",
        groupId = "trackops-orders",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleInventoryReserved(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            String message = record.value();
            UUID orderId = UUID.fromString(record.key());
            String topic = record.topic();
            
            log.info("Received INVENTORY_RESERVED event for order: {} from topic: {}", orderId, topic);
            log.debug("Message content: {}", message);

            InventoryReservedEvent event = objectMapper.readValue(message, InventoryReservedEvent.class);
            orderEventProcessor.processOrderEvent(event);

            log.info("Successfully processed INVENTORY_RESERVED event for order: {}", orderId);
            acknowledgment.acknowledge();

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize INVENTORY_RESERVED event for order: {} - Invalid JSON format", record.key(), e);
            acknowledgment.acknowledge(); // Acknowledge malformed messages to avoid infinite retry
        } catch (Exception e) {
            log.error("Failed to process INVENTORY_RESERVED event for order: {}", record.key(), e);
            // Don't acknowledge - let Kafka retry the message
            throw e; // Re-throw to trigger retry mechanism
        }
    }

    @KafkaListener(
        topics = "INVENTORY_RESERVATION_FAILED",
        groupId = "trackops-orders",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleInventoryReservationFailed(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            String message = record.value();
            UUID orderId = UUID.fromString(record.key());
            String topic = record.topic();
            
            log.info("Received INVENTORY_RESERVATION_FAILED event for order: {} from topic: {}", orderId, topic);
            log.debug("Message content: {}", message);

            InventoryReservationFailedEvent event = objectMapper.readValue(message, InventoryReservationFailedEvent.class);
            orderEventProcessor.processOrderEvent(event);

            log.info("Successfully processed INVENTORY_RESERVATION_FAILED event for order: {}", orderId);
            acknowledgment.acknowledge();

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize INVENTORY_RESERVATION_FAILED event for order: {} - Invalid JSON format", record.key(), e);
            acknowledgment.acknowledge(); // Acknowledge malformed messages to avoid infinite retry
        } catch (Exception e) {
            log.error("Failed to process INVENTORY_RESERVATION_FAILED event for order: {}", record.key(), e);
            // Don't acknowledge - let Kafka retry the message
            throw e; // Re-throw to trigger retry mechanism
        }
    }

    @KafkaListener(
        topics = "INVENTORY_RELEASED",
        groupId = "trackops-orders",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleInventoryReleased(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            String message = record.value();
            UUID orderId = UUID.fromString(record.key());
            String topic = record.topic();
            
            log.info("Received INVENTORY_RELEASED event for order: {} from topic: {}", orderId, topic);
            log.debug("Message content: {}", message);

            InventoryReleasedEvent event = objectMapper.readValue(message, InventoryReleasedEvent.class);
            orderEventProcessor.processOrderEvent(event);

            log.info("Successfully processed INVENTORY_RELEASED event for order: {}", orderId);
            acknowledgment.acknowledge();

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize INVENTORY_RELEASED event for order: {} - Invalid JSON format", record.key(), e);
            acknowledgment.acknowledge(); // Acknowledge malformed messages to avoid infinite retry
        } catch (Exception e) {
            log.error("Failed to process INVENTORY_RELEASED event for order: {}", record.key(), e);
            // Don't acknowledge - let Kafka retry the message
            throw e; // Re-throw to trigger retry mechanism
        }
    }
}