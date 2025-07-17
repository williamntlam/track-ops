package com.trackops.server.domain.events.orders;

import com.trackops.server.domain.model.events.Event;
import java.util.UUID;

public abstract class OrderEvent extends Event {

    private final UUID orderId;

    public OrderEvent(UUID orderId, String eventType) {

        super(eventType);
        this.orderId = orderId; 

    }

    public UUID getOrderId() {
        return orderId;
    }

}