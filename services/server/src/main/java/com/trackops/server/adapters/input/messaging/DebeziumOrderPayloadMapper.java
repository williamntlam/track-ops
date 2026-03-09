package com.trackops.server.adapters.input.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.trackops.server.adapters.input.web.dto.AddressDTO;
import com.trackops.server.adapters.input.web.dto.OrderResponse;
import com.trackops.server.domain.model.enums.OrderStatus;
import com.trackops.server.domain.model.orders.Address;
import com.trackops.server.domain.model.orders.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Maps Debezium CDC "after" payload (JSON) to Order and OrderResponse for cache updates.
 * Debezium column names are snake_case (e.g. customer_id, total_amount).
 */
@Slf4j
@Component
public class DebeziumOrderPayloadMapper {

    /**
     * Build an Order domain entity from Debezium "after" node.
     * Returns null if payload is invalid.
     */
    public Order orderFromAfter(JsonNode after) {
        if (after == null || after.isMissingNode()) {
            return null;
        }
        try {
            UUID id = uuid(after, "id");
            UUID customerId = uuid(after, "customer_id");
            OrderStatus status = status(after, "status");
            BigDecimal totalAmount = decimal(after, "total_amount");
            Address address = addressFromAfter(after);
            String deliveryInstructions = text(after, "delivery_instructions");
            Instant createdAt = instant(after, "created_at");
            Instant updatedAt = instant(after, "updated_at");
            if (id == null || customerId == null || status == null || totalAmount == null || updatedAt == null) {
                return null;
            }
            if (createdAt == null) {
                createdAt = updatedAt;
            }
            Order order = new Order(id, customerId, status, totalAmount, address, deliveryInstructions, createdAt, updatedAt);
            Long version = after.has("version") && !after.get("version").isNull() ? after.get("version").asLong() : null;
            if (version != null) {
                order.setVersion(version);
            }
            return order;
        } catch (Exception e) {
            log.warn("Failed to map Debezium after to Order: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Build an OrderResponse from Debezium "after" node (for cache).
     * Returns null if payload is invalid.
     */
    public OrderResponse orderResponseFromAfter(JsonNode after) {
        Order order = orderFromAfter(after);
        if (order == null) {
            return null;
        }
        AddressDTO addressDto = addressDtoFromAfter(after);
        Instant createdAt = order.getCreatedAt();
        Instant updatedAt = order.getUpdatedAt();
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                addressDto,
                order.getDeliveryInstructions(),
                createdAt != null ? createdAt : updatedAt,
                updatedAt
        );
    }

    private static UUID uuid(JsonNode n, String field) {
        if (!n.has(field) || n.get(field).isNull()) return null;
        String s = n.get(field).asText();
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static OrderStatus status(JsonNode n, String field) {
        if (!n.has(field) || n.get(field).isNull()) return null;
        String s = n.get(field).asText().toUpperCase();
        try {
            return OrderStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static BigDecimal decimal(JsonNode n, String field) {
        if (!n.has(field) || n.get(field).isNull()) return null;
        JsonNode v = n.get(field);
        if (v.isNumber()) {
            return BigDecimal.valueOf(v.asDouble());
        }
        try {
            return new BigDecimal(v.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(JsonNode n, String field) {
        if (!n.has(field) || n.get(field).isNull()) return null;
        return n.get(field).asText();
    }

    private static Instant instant(JsonNode n, String field) {
        if (!n.has(field) || n.get(field).isNull()) return null;
        JsonNode v = n.get(field);
        if (v.isNumber()) {
            return Instant.ofEpochMilli(v.asLong());
        }
        String s = v.asText();
        try {
            return Instant.parse(s);
        } catch (Exception e) {
            try {
                return Instant.ofEpochMilli(Long.parseLong(s));
            } catch (NumberFormatException e2) {
                return null;
            }
        }
    }

    private static Address addressFromAfter(JsonNode after) {
        String street = text(after, "street_address");
        String city = text(after, "city");
        String state = text(after, "state");
        String postalCode = text(after, "postal_code");
        String country = text(after, "country");
        String phone = text(after, "phone_number");
        return new Address(street, city, state, postalCode, country, phone);
    }

    private static AddressDTO addressDtoFromAfter(JsonNode after) {
        String street = text(after, "street_address");
        String city = text(after, "city");
        String state = text(after, "state");
        String postalCode = text(after, "postal_code");
        String country = text(after, "country");
        String phone = text(after, "phone_number");
        return new AddressDTO(street, city, state, postalCode, country, phone);
    }
}
