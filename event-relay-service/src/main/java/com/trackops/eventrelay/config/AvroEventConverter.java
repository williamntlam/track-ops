package com.trackops.eventrelay.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to convert JSON payloads from outbox events to Avro GenericRecord.
 * This allows the event-relay-service to send validated Avro messages to Kafka.
 */
@Slf4j
@Component
public class AvroEventConverter {

    private final SchemaRegistryService schemaRegistryService;
    private final ObjectMapper objectMapper;
    private final Map<String, Schema> schemaCache = new ConcurrentHashMap<>();
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public AvroEventConverter(SchemaRegistryService schemaRegistryService, ObjectMapper objectMapper) {
        this.schemaRegistryService = schemaRegistryService;
        this.objectMapper = objectMapper;
    }

    /**
     * Converts a JSON payload to Avro GenericRecord based on event type.
     * 
     * @param eventType The event type (e.g., "ORDER_CREATED")
     * @param jsonPayload The JSON payload string
     * @return Avro GenericRecord
     */
    public GenericRecord jsonToAvro(String eventType, String jsonPayload) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonPayload);
            String subject = eventType + "-value";
            Schema schema = getSchema(subject);
            
            GenericRecord record = new GenericData.Record(schema);
            
            // Convert based on event type
            switch (eventType) {
                case "ORDER_CREATED":
                    convertOrderCreated(jsonNode, record);
                    break;
                case "ORDER_STATUS_UPDATED":
                    convertOrderStatusUpdated(jsonNode, record);
                    break;
                case "ORDER_DELIVERED":
                    convertOrderDelivered(jsonNode, record);
                    break;
                case "ORDER_CANCELLED":
                    convertOrderCancelled(jsonNode, record);
                    break;
                case "INVENTORY_RESERVED":
                    convertInventoryReserved(jsonNode, record);
                    break;
                case "INVENTORY_RESERVATION_FAILED":
                    convertInventoryReservationFailed(jsonNode, record);
                    break;
                case "INVENTORY_RELEASED":
                    convertInventoryReleased(jsonNode, record);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown event type: " + eventType);
            }
            
            return record;
        } catch (Exception e) {
            log.error("Failed to convert JSON to Avro for event type {}: {}", eventType, e.getMessage(), e);
            throw new RuntimeException("Failed to convert JSON to Avro: " + e.getMessage(), e);
        }
    }

    private void convertOrderCreated(JsonNode json, GenericRecord record) {
        record.put("orderId", json.get("orderId").asText());
        record.put("eventType", "ORDER_CREATED");
        record.put("createdBy", json.has("createdBy") ? json.get("createdBy").asText() : "");
    }

    private void convertOrderStatusUpdated(JsonNode json, GenericRecord record) {
        record.put("orderId", json.get("orderId").asText());
        record.put("eventType", "ORDER_STATUS_UPDATED");
        record.put("previousStatus", json.has("previousStatus") ? json.get("previousStatus").asText() : "");
        record.put("newStatus", json.has("newStatus") ? json.get("newStatus").asText() : "");
        record.put("expectedVersion", json.has("expectedVersion") && !json.get("expectedVersion").isNull() 
            ? json.get("expectedVersion").asLong() : null);
    }

    private void convertOrderDelivered(JsonNode json, GenericRecord record) {
        record.put("orderId", json.get("orderId").asText());
        record.put("eventType", "ORDER_DELIVERED");
        record.put("deliveredAt", json.has("deliveredAt") && !json.get("deliveredAt").isNull() 
            ? json.get("deliveredAt").asText() : null);
    }

    private void convertOrderCancelled(JsonNode json, GenericRecord record) {
        record.put("orderId", json.get("orderId").asText());
        record.put("eventType", "ORDER_CANCELLED");
        record.put("cancelledBy", json.has("cancelledBy") ? json.get("cancelledBy").asText() : "");
        record.put("cancellationReason", json.has("cancellationReason") ? json.get("cancellationReason").asText() : "");
        record.put("cancelledAt", json.has("cancelledAt") && !json.get("cancelledAt").isNull() 
            ? json.get("cancelledAt").asText() : null);
    }

    private void convertInventoryReserved(JsonNode json, GenericRecord record) {
        record.put("eventId", json.has("eventId") ? json.get("eventId").asText() : "");
        record.put("eventType", "INVENTORY_RESERVED");
        record.put("orderId", json.get("orderId").asText());
        record.put("timestamp", json.has("timestamp") && !json.get("timestamp").isNull() 
            ? json.get("timestamp").asText() : null);
        record.put("reservationId", json.has("reservationId") ? json.get("reservationId").asText() : "");
        
        if (json.has("reservedItems")) {
            List<GenericRecord> items = new ArrayList<>();
            Schema itemSchema = record.getSchema().getField("reservedItems").schema().getElementType();
            for (JsonNode item : json.get("reservedItems")) {
                GenericRecord itemRecord = new GenericData.Record(itemSchema);
                itemRecord.put("productId", item.get("productId").asText());
                itemRecord.put("productName", item.get("productName").asText());
                itemRecord.put("quantity", item.get("quantity").asInt());
                itemRecord.put("unitPrice", item.get("unitPrice").asText());
                items.add(itemRecord);
            }
            record.put("reservedItems", items);
        }
    }

    private void convertInventoryReservationFailed(JsonNode json, GenericRecord record) {
        record.put("eventId", json.has("eventId") ? json.get("eventId").asText() : "");
        record.put("eventType", "INVENTORY_RESERVATION_FAILED");
        record.put("orderId", json.get("orderId").asText());
        record.put("timestamp", json.has("timestamp") && !json.get("timestamp").isNull() 
            ? json.get("timestamp").asText() : null);
        record.put("reason", json.has("reason") ? json.get("reason").asText() : "");
        
        if (json.has("failedItems")) {
            List<GenericRecord> items = new ArrayList<>();
            Schema itemSchema = record.getSchema().getField("failedItems").schema().getElementType();
            for (JsonNode item : json.get("failedItems")) {
                GenericRecord itemRecord = new GenericData.Record(itemSchema);
                itemRecord.put("productId", item.get("productId").asText());
                itemRecord.put("productName", item.get("productName").asText());
                itemRecord.put("requestedQuantity", item.get("requestedQuantity").asInt());
                itemRecord.put("availableQuantity", item.get("availableQuantity").asInt());
                itemRecord.put("failureReason", item.get("failureReason").asText());
                items.add(itemRecord);
            }
            record.put("failedItems", items);
        }
    }

    private void convertInventoryReleased(JsonNode json, GenericRecord record) {
        record.put("eventId", json.has("eventId") ? json.get("eventId").asText() : "");
        record.put("eventType", "INVENTORY_RELEASED");
        record.put("orderId", json.get("orderId").asText());
        record.put("timestamp", json.has("timestamp") && !json.get("timestamp").isNull() 
            ? json.get("timestamp").asText() : null);
        record.put("reservationId", json.has("reservationId") ? json.get("reservationId").asText() : "");
        record.put("reason", json.has("reason") ? json.get("reason").asText() : "");
        
        if (json.has("releasedItems")) {
            List<GenericRecord> items = new ArrayList<>();
            Schema itemSchema = record.getSchema().getField("releasedItems").schema().getElementType();
            for (JsonNode item : json.get("releasedItems")) {
                GenericRecord itemRecord = new GenericData.Record(itemSchema);
                itemRecord.put("productId", item.get("productId").asText());
                itemRecord.put("productName", item.get("productName").asText());
                itemRecord.put("quantity", item.get("quantity").asInt());
                items.add(itemRecord);
            }
            record.put("releasedItems", items);
        }
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
