package com.trackops.server.domain.events.orders;

import java.util.UUID;

public class OrderCreatedEvent extends OrderEvent {

    private final String createdBy;

    public OrderCreatedAt(UUID orderId, String createdBy) {

        super(orderId, "ORDER_CREATED")
        this.createdBy = createdBy;

    }

    public String getCreatedBy() {

        return createdBy;

    }

}