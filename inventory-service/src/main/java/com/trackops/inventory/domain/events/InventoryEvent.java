package com.trackops.inventory.domain.events;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@type")
public abstract class InventoryEvent {
    
    private final String eventId;
    private final String eventType;
    private final UUID orderId;
    private final Instant timestamp;
    
    protected InventoryEvent(String eventType, UUID orderId) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.orderId = orderId;
        this.timestamp = Instant.now();
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public UUID getOrderId() {
        return orderId;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
}
