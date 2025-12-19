package com.trackops.inventory.config;

import com.trackops.inventory.domain.events.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to convert domain event POJOs to Avro GenericRecord for inventory events.
 * This allows us to use Avro serialization with Schema Registry while keeping
 * our domain models as simple POJOs.
 */
@Slf4j
@Component
public class AvroEventConverter {

    private final SchemaRegistryService schemaRegistryService;
    private final Map<String, Schema> schemaCache = new ConcurrentHashMap<>();

    public AvroEventConverter(SchemaRegistryService schemaRegistryService) {
        this.schemaRegistryService = schemaRegistryService;
    }

    /**
     * Converts an InventoryReservedEvent to Avro GenericRecord.
     */
    public GenericRecord toAvro(InventoryReservedEvent event) {
        String subject = "INVENTORY_RESERVED-value";
        Schema schema = getSchema(subject);
        
        GenericRecord record = new GenericData.Record(schema);
        record.put("eventId", event.getEventId());
        record.put("eventType", "INVENTORY_RESERVED");
        record.put("orderId", event.getOrderId().toString());
        record.put("timestamp", event.getTimestamp() != null ? event.getTimestamp().toString() : null);
        record.put("reservationId", event.getReservationId());
        
        // Convert reservedItems list
        List<GenericRecord> reservedItemsList = new ArrayList<>();
        Schema reservedItemSchema = schema.getField("reservedItems").schema().getElementType();
        for (InventoryReservedEvent.ReservedItem item : event.getReservedItems()) {
            GenericRecord itemRecord = new GenericData.Record(reservedItemSchema);
            itemRecord.put("productId", item.getProductId());
            itemRecord.put("productName", item.getProductName());
            itemRecord.put("quantity", item.getQuantity());
            itemRecord.put("unitPrice", item.getUnitPrice());
            reservedItemsList.add(itemRecord);
        }
        record.put("reservedItems", reservedItemsList);
        
        return record;
    }

    /**
     * Converts an InventoryReservationFailedEvent to Avro GenericRecord.
     */
    public GenericRecord toAvro(InventoryReservationFailedEvent event) {
        String subject = "INVENTORY_RESERVATION_FAILED-value";
        Schema schema = getSchema(subject);
        
        GenericRecord record = new GenericData.Record(schema);
        record.put("eventId", event.getEventId());
        record.put("eventType", "INVENTORY_RESERVATION_FAILED");
        record.put("orderId", event.getOrderId().toString());
        record.put("timestamp", event.getTimestamp() != null ? event.getTimestamp().toString() : null);
        record.put("reason", event.getReason());
        
        // Convert failedItems list
        List<GenericRecord> failedItemsList = new ArrayList<>();
        Schema failedItemSchema = schema.getField("failedItems").schema().getElementType();
        for (InventoryReservationFailedEvent.FailedItem item : event.getFailedItems()) {
            GenericRecord itemRecord = new GenericData.Record(failedItemSchema);
            itemRecord.put("productId", item.getProductId());
            itemRecord.put("productName", item.getProductName());
            itemRecord.put("requestedQuantity", item.getRequestedQuantity());
            itemRecord.put("availableQuantity", item.getAvailableQuantity());
            itemRecord.put("failureReason", item.getFailureReason());
            failedItemsList.add(itemRecord);
        }
        record.put("failedItems", failedItemsList);
        
        return record;
    }

    /**
     * Converts an InventoryReleasedEvent to Avro GenericRecord.
     */
    public GenericRecord toAvro(InventoryReleasedEvent event) {
        String subject = "INVENTORY_RELEASED-value";
        Schema schema = getSchema(subject);
        
        GenericRecord record = new GenericData.Record(schema);
        record.put("eventId", event.getEventId());
        record.put("eventType", "INVENTORY_RELEASED");
        record.put("orderId", event.getOrderId().toString());
        record.put("timestamp", event.getTimestamp() != null ? event.getTimestamp().toString() : null);
        record.put("reservationId", event.getReservationId());
        record.put("reason", event.getReason());
        
        // Convert releasedItems list
        List<GenericRecord> releasedItemsList = new ArrayList<>();
        Schema releasedItemSchema = schema.getField("releasedItems").schema().getElementType();
        for (InventoryReleasedEvent.ReleasedItem item : event.getReleasedItems()) {
            GenericRecord itemRecord = new GenericData.Record(releasedItemSchema);
            itemRecord.put("productId", item.getProductId());
            itemRecord.put("productName", item.getProductName());
            itemRecord.put("quantity", item.getQuantity());
            releasedItemsList.add(itemRecord);
        }
        record.put("releasedItems", releasedItemsList);
        
        return record;
    }

    /**
     * Converts an Avro GenericRecord to InventoryReservedEvent.
     */
    public InventoryReservedEvent fromAvroReserved(GenericRecord record) {
        UUID orderId = UUID.fromString(record.get("orderId").toString());
        String reservationId = record.get("reservationId").toString();
        
        // Convert reservedItems list
        @SuppressWarnings("unchecked")
        List<GenericRecord> reservedItemsList = (List<GenericRecord>) record.get("reservedItems");
        List<InventoryReservedEvent.ReservedItem> items = new ArrayList<>();
        for (GenericRecord itemRecord : reservedItemsList) {
            items.add(new InventoryReservedEvent.ReservedItem(
                itemRecord.get("productId").toString(),
                itemRecord.get("productName").toString(),
                (Integer) itemRecord.get("quantity"),
                itemRecord.get("unitPrice").toString()
            ));
        }
        
        return new InventoryReservedEvent(orderId, reservationId, items);
    }

    /**
     * Converts an Avro GenericRecord to InventoryReservationFailedEvent.
     */
    public InventoryReservationFailedEvent fromAvroReservationFailed(GenericRecord record) {
        UUID orderId = UUID.fromString(record.get("orderId").toString());
        String reason = record.get("reason").toString();
        
        // Convert failedItems list
        @SuppressWarnings("unchecked")
        List<GenericRecord> failedItemsList = (List<GenericRecord>) record.get("failedItems");
        List<InventoryReservationFailedEvent.FailedItem> items = new ArrayList<>();
        for (GenericRecord itemRecord : failedItemsList) {
            items.add(new InventoryReservationFailedEvent.FailedItem(
                itemRecord.get("productId").toString(),
                itemRecord.get("productName").toString(),
                (Integer) itemRecord.get("requestedQuantity"),
                (Integer) itemRecord.get("availableQuantity"),
                itemRecord.get("failureReason").toString()
            ));
        }
        
        return new InventoryReservationFailedEvent(orderId, reason, items);
    }

    /**
     * Converts an Avro GenericRecord to InventoryReleasedEvent.
     */
    public InventoryReleasedEvent fromAvroReleased(GenericRecord record) {
        UUID orderId = UUID.fromString(record.get("orderId").toString());
        String reservationId = record.get("reservationId").toString();
        String reason = record.get("reason").toString();
        
        // Convert releasedItems list
        @SuppressWarnings("unchecked")
        List<GenericRecord> releasedItemsList = (List<GenericRecord>) record.get("releasedItems");
        List<InventoryReleasedEvent.ReleasedItem> items = new ArrayList<>();
        for (GenericRecord itemRecord : releasedItemsList) {
            items.add(new InventoryReleasedEvent.ReleasedItem(
                itemRecord.get("productId").toString(),
                itemRecord.get("productName").toString(),
                (Integer) itemRecord.get("quantity")
            ));
        }
        
        return new InventoryReleasedEvent(orderId, reservationId, items, reason);
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
