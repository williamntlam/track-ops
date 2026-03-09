package com.trackops.server.config;

import com.trackops.server.domain.events.orders.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to convert domain event POJOs to Avro GenericRecord.
 * This allows us to use Avro serialization with Schema Registry while keeping
 * our domain models as simple POJOs.
 */
@Slf4j
@Component
public class AvroEventConverter {

    private final SchemaRegistryService schemaRegistryService;
    private final Map<String, Schema> schemaCache = new ConcurrentHashMap<>();
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public AvroEventConverter(SchemaRegistryService schemaRegistryService) {
        this.schemaRegistryService = schemaRegistryService;
    }

    /**
     * Converts an OrderCreatedEvent to Avro GenericRecord.
     */
    public GenericRecord toAvro(OrderCreatedEvent event) {
        String subject = "ORDER_CREATED-value";
        Schema schema = getSchema(subject);
        
        GenericRecord record = new GenericData.Record(schema);
        record.put("orderId", event.getOrderId().toString());
        record.put("eventType", "ORDER_CREATED");
        record.put("createdBy", event.getCreatedBy());
        
        return record;
    }

    /**
     * Converts an OrderStatusUpdatedEvent to Avro GenericRecord.
     */
    public GenericRecord toAvro(OrderStatusUpdatedEvent event) {
        String subject = "ORDER_STATUS_UPDATED-value";
        Schema schema = getSchema(subject);
        
        GenericRecord record = new GenericData.Record(schema);
        record.put("orderId", event.getOrderId().toString());
        record.put("eventType", "ORDER_STATUS_UPDATED");
        record.put("previousStatus", event.getPreviousStatus().toString());
        record.put("newStatus", event.getNewStatus().toString());
        record.put("expectedVersion", event.getExpectedVersion());
        
        return record;
    }

    /**
     * Converts an OrderDeliveredEvent to Avro GenericRecord.
     */
    public GenericRecord toAvro(OrderDeliveredEvent event) {
        String subject = "ORDER_DELIVERED-value";
        Schema schema = getSchema(subject);
        
        GenericRecord record = new GenericData.Record(schema);
        record.put("orderId", event.getOrderId().toString());
        record.put("eventType", "ORDER_DELIVERED");
        record.put("deliveredAt", event.getDeliveredAt() != null 
            ? event.getDeliveredAt().format(ISO_FORMATTER) : null);
        
        return record;
    }

    /**
     * Converts an OrderCancelledEvent to Avro GenericRecord.
     */
    public GenericRecord toAvro(OrderCancelledEvent event) {
        String subject = "ORDER_CANCELLED-value";
        Schema schema = getSchema(subject);
        
        GenericRecord record = new GenericData.Record(schema);
        record.put("orderId", event.getOrderId().toString());
        record.put("eventType", "ORDER_CANCELLED");
        record.put("cancelledBy", event.getCancelledBy());
        record.put("cancellationReason", event.getCancellationReason());
        record.put("cancelledAt", event.getCancelledAt() != null 
            ? event.getCancelledAt().format(ISO_FORMATTER) : null);
        
        return record;
    }

    /**
     * Converts an Avro GenericRecord to OrderCreatedEvent.
     */
    public OrderCreatedEvent fromAvro(GenericRecord record) {
        return new OrderCreatedEvent(
            UUID.fromString(record.get("orderId").toString()),
            record.get("createdBy").toString()
        );
    }

    /**
     * Converts an Avro GenericRecord to OrderStatusUpdatedEvent.
     */
    public OrderStatusUpdatedEvent fromAvroStatusUpdated(GenericRecord record) {
        return new OrderStatusUpdatedEvent(
            UUID.fromString(record.get("orderId").toString()),
            com.trackops.server.domain.model.enums.OrderStatus.valueOf(record.get("previousStatus").toString()),
            com.trackops.server.domain.model.enums.OrderStatus.valueOf(record.get("newStatus").toString()),
            record.get("expectedVersion") != null ? (Long) record.get("expectedVersion") : null
        );
    }

    /**
     * Converts an Avro GenericRecord to OrderDeliveredEvent.
     */
    public OrderDeliveredEvent fromAvroDelivered(GenericRecord record) {
        LocalDateTime deliveredAt = null;
        if (record.get("deliveredAt") != null) {
            deliveredAt = LocalDateTime.parse(record.get("deliveredAt").toString(), ISO_FORMATTER);
        }
        return new OrderDeliveredEvent(
            UUID.fromString(record.get("orderId").toString()),
            deliveredAt
        );
    }

    /**
     * Converts an Avro GenericRecord to OrderCancelledEvent.
     * Note: cancelledAt is set automatically in the constructor to LocalDateTime.now()
     */
    public OrderCancelledEvent fromAvroCancelled(GenericRecord record) {
        return new OrderCancelledEvent(
            UUID.fromString(record.get("orderId").toString()),
            record.get("cancelledBy").toString(),
            record.get("cancellationReason").toString()
        );
    }

    /**
     * Gets the schema for a subject, with caching.
     */
    private Schema getSchema(String subject) {
        return schemaCache.computeIfAbsent(subject, s -> {
            try {
                return schemaRegistryService.getLatestSchema(s);
            } catch (Exception e) {
                log.error("Failed to get schema for subject: {}", subject, e);
                throw new RuntimeException("Failed to get schema for subject: " + subject, e);
            }
        });
    }
}
