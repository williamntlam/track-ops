package com.trackops.inventory.adapters.output.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.inventory.domain.events.InventoryReservedEvent;
import com.trackops.inventory.domain.events.InventoryReservationFailedEvent;
import com.trackops.inventory.domain.events.InventoryReleasedEvent;
import com.trackops.inventory.ports.output.events.InventoryEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class KafkaInventoryEventProducer implements InventoryEventProducer {

    private final KafkaTemplate<UUID, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaInventoryEventProducer(KafkaTemplate<UUID, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
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
            String value = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(topic, key, value);
            log.debug("Successfully published event {} for order {}", topic, key);

        } catch (JsonProcessingException err) {
            log.error("Failed to serialize event: {}", err.getMessage(), err);
        } catch (Exception err) {
            log.error("Failed to publish event: {}", err.getMessage(), err);
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
