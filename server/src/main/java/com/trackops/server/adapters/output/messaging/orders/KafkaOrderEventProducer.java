package com.trackops.server.adapters.output.messaging.orders;

import com.trackops.server.config.AvroEventConverter;
import com.trackops.server.config.SchemaRegistryService;
import com.trackops.server.domain.model.OperationResult;
import com.trackops.server.domain.events.orders.OrderCancelledEvent;
import com.trackops.server.domain.events.orders.OrderCreatedEvent;
import com.trackops.server.domain.events.orders.OrderDeliveredEvent;
import com.trackops.server.domain.events.orders.OrderEvent;
import com.trackops.server.domain.events.orders.OrderStatusUpdatedEvent;
import com.trackops.server.ports.output.events.orders.OrderEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Slf4j
@Service
public class KafkaOrderEventProducer implements OrderEventProducer {

    private final KafkaTemplate<UUID, GenericRecord> kafkaTemplate;
    private final AvroEventConverter avroEventConverter;

    public KafkaOrderEventProducer(
            KafkaTemplate<UUID, GenericRecord> kafkaTemplate,
            AvroEventConverter avroEventConverter) {
        this.kafkaTemplate = kafkaTemplate;
        this.avroEventConverter = avroEventConverter;
    }

    @Override
    public OperationResult publishOrderCreated(OrderCreatedEvent event) {
        return publishEvent(event);
    }

    @Override
    public OperationResult publishOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        return publishEvent(event);
    }

    @Override
    public OperationResult publishOrderDelivered(OrderDeliveredEvent event) {
        return publishEvent(event);
    }

    @Override
    public OperationResult publishOrderCancelled(OrderCancelledEvent event) {
        return publishEvent(event);
    }

    private OperationResult publishEvent(OrderEvent event) {
        try {
            String topic = event.getEventType();
            UUID key = event.getOrderId();
            
            // Convert event to Avro GenericRecord
            GenericRecord avroRecord = convertToAvro(event);
            
            // The Confluent Avro serializer will automatically register the schema
            // if it doesn't exist and validate compatibility
            kafkaTemplate.send(topic, key, avroRecord);
            log.debug("Successfully published Avro event {} for order {}", event.getEventType(), event.getOrderId());
            return OperationResult.success();

        } catch (Exception err) {
            log.error("Failed to publish event for order {}: {}", 
                    event.getOrderId(), err.getMessage(), err);
            return OperationResult.failure("Failed to publish event: " + err.getMessage());
        }
    }

    private GenericRecord convertToAvro(OrderEvent event) {
        if (event instanceof OrderCreatedEvent) {
            return avroEventConverter.toAvro((OrderCreatedEvent) event);
        } else if (event instanceof OrderStatusUpdatedEvent) {
            return avroEventConverter.toAvro((OrderStatusUpdatedEvent) event);
        } else if (event instanceof OrderDeliveredEvent) {
            return avroEventConverter.toAvro((OrderDeliveredEvent) event);
        } else if (event instanceof OrderCancelledEvent) {
            return avroEventConverter.toAvro((OrderCancelledEvent) event);
        } else {
            throw new IllegalArgumentException("Unknown event type: " + event.getClass().getSimpleName());
        }
    }

}

