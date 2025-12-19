package com.trackops.inventory.adapters.input.messaging;

import com.trackops.inventory.domain.events.OrderEvent;
import com.trackops.inventory.ports.input.events.OrderEventProcessorPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class KafkaOrderEventConsumer {

    private final OrderEventProcessorPort orderEventProcessor;

    public KafkaOrderEventConsumer(OrderEventProcessorPort orderEventProcessor) {
        this.orderEventProcessor = orderEventProcessor;
    }

    @KafkaListener(
        topics = "ORDER_CREATED",
        groupId = "inventory-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreated(ConsumerRecord<UUID, GenericRecord> record, Acknowledgment acknowledgment) {
        try {
            GenericRecord avroRecord = record.value();
            UUID orderId = record.key();
            String topic = record.topic();
            
            log.info("Received ORDER_CREATED event for order: {} from topic: {}", orderId, topic);
            log.debug("Avro record: {}", avroRecord);

            // Create a simple OrderEvent for processing
            // The Avro record is validated by Schema Registry, so we can trust the data
            OrderEvent event = new OrderEvent("ORDER_CREATED", orderId) {};
            orderEventProcessor.processOrderEvent(event);

            log.info("Successfully processed ORDER_CREATED event for order: {}", orderId);
            acknowledgment.acknowledge();

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
    public void handleOrderCancelled(ConsumerRecord<UUID, GenericRecord> record, Acknowledgment acknowledgment) {
        try {
            GenericRecord avroRecord = record.value();
            UUID orderId = record.key();
            String topic = record.topic();
            
            log.info("Received ORDER_CANCELLED event for order: {} from topic: {}", orderId, topic);
            log.debug("Avro record: {}", avroRecord);

            // Create a simple OrderEvent for processing
            // The Avro record is validated by Schema Registry, so we can trust the data
            OrderEvent event = new OrderEvent("ORDER_CANCELLED", orderId) {};
            orderEventProcessor.processOrderEvent(event);

            log.info("Successfully processed ORDER_CANCELLED event for order: {}", orderId);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process ORDER_CANCELLED event for order: {}", record.key(), e);
            // Don't acknowledge - let Kafka retry the message
            throw e; // Re-throw to trigger retry mechanism
        }
    }
}
