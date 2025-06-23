package com.trackops.server.domain.events.orders;

import java.time.Instant;
import java.util.UUID;

public abstract class OrderEvent {

    private final UUID eventId;
    private final UUID orderId;
    private final Instant timestamp;
    private final String eventType;

    public OrderEvent(UUID orderId, String eventType) {

        this.eventId = UUID.randomUUID(); // There might be a problem here.
        this.orderId = orderId;
        this.timestamp = Instant.now();
        this.eventType = eventType;

    }

    public String getEventId() {
        return eventId;
    }

    public String getOrderId() {
        return orderId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getEventType() {
        return eventType;
    }

}