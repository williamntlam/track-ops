package com.trackops.inventory.adapters.output.messaging;

import com.trackops.inventory.config.AvroEventConverter;
import com.trackops.inventory.domain.events.InventoryReservedEvent;
import com.trackops.inventory.domain.events.InventoryReservationFailedEvent;
import com.trackops.inventory.domain.events.InventoryReleasedEvent;
import com.trackops.inventory.ports.output.events.InventoryEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class KafkaInventoryEventProducer implements InventoryEventProducer {

    private final KafkaTemplate<UUID, GenericRecord> kafkaTemplate;
    private final AvroEventConverter avroEventConverter;

    public KafkaInventoryEventProducer(
            KafkaTemplate<UUID, GenericRecord> kafkaTemplate,
            AvroEventConverter avroEventConverter) {
        this.kafkaTemplate = kafkaTemplate;
        this.avroEventConverter = avroEventConverter;
    }

    @Override
    public void publishInventoryReserved(InventoryReservedEvent event) {
        publishEvent(event);
    }

    @Override
    public void publishInventoryReservationFailed(InventoryReservationFailedEvent event) {
        publishEvent(event);
    }

    @Override
    public void publishInventoryReleased(InventoryReleasedEvent event) {
        publishEvent(event);
    }

    private void publishEvent(Object event) {
        try {
            String topic = getTopicForEvent(event);
            UUID key = getOrderIdFromEvent(event);
            
            // Convert event to Avro GenericRecord
            GenericRecord avroRecord = convertToAvro(event);
            
            // The Confluent Avro serializer will automatically register the schema
            // if it doesn't exist and validate compatibility
            kafkaTemplate.send(topic, key, avroRecord);
            log.debug("Successfully published Avro event {} for order {}", topic, key);

        } catch (Exception err) {
            log.error("Failed to publish event: {}", err.getMessage(), err);
        }
    }

    private GenericRecord convertToAvro(Object event) {
        if (event instanceof InventoryReservedEvent) {
            return avroEventConverter.toAvro((InventoryReservedEvent) event);
        } else if (event instanceof InventoryReservationFailedEvent) {
            return avroEventConverter.toAvro((InventoryReservationFailedEvent) event);
        } else if (event instanceof InventoryReleasedEvent) {
            return avroEventConverter.toAvro((InventoryReleasedEvent) event);
        } else {
            throw new IllegalArgumentException("Unknown event type: " + event.getClass().getSimpleName());
        }
    }

    private String getTopicForEvent(Object event) {
        if (event instanceof InventoryReservedEvent) {
            return "INVENTORY_RESERVED";
        } else if (event instanceof InventoryReservationFailedEvent) {
            return "INVENTORY_RESERVATION_FAILED";
        } else if (event instanceof InventoryReleasedEvent) {
            return "INVENTORY_RELEASED";
        }
        throw new IllegalArgumentException("Unknown event type: " + event.getClass().getSimpleName());
    }

    private UUID getOrderIdFromEvent(Object event) {
        if (event instanceof InventoryReservedEvent) {
            return ((InventoryReservedEvent) event).getOrderId();
        } else if (event instanceof InventoryReservationFailedEvent) {
            return ((InventoryReservationFailedEvent) event).getOrderId();
        } else if (event instanceof InventoryReleasedEvent) {
            return ((InventoryReleasedEvent) event).getOrderId();
        }
        throw new IllegalArgumentException("Unknown event type: " + event.getClass().getSimpleName());
    }
}
