package com.trackops.server.domain.events.orders;

import java.util.UUID;

public class OrderStatusUpdatedEvent extends OrderEvent {

    private final OrderStatus previousStatus;
    private final OrderStatus newStatus;

    public OrderStatusUpdatedEvent(UUID orderId, OrderStatus previousStatus, OrderStatus newStatus) {
        super(orderId, "ORDER_STATUS_UPDATED");
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public OrderStatus getPreviousStatus() {
        return previousStatus;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }
}