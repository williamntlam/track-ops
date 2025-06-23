package com.trackops.server.domain.events.orders;

import java.time.LocalDateTime;
import java.util.UUID;

public class OrderDeliveredEvent extends OrderEvent {

    private final LocalDateTime deliveredAt;

    public OrderDeliveredEvent(UUID orderId, LocalDateTime deliveredAt) {

        super(orderId, "ORDER_DELIVERED");
        this.deliveredAt = deliveredAt;

    }

    public LocalDateTime getDeliveredAt() {

        return deliveredAt;

    }

}