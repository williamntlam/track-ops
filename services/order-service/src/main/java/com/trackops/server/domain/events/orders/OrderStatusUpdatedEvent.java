package com.trackops.server.domain.events.orders;

import java.util.UUID;
import com.trackops.server.domain.model.enums.OrderStatus;

public class OrderStatusUpdatedEvent extends OrderEvent {

    private final OrderStatus previousStatus;
    private final OrderStatus newStatus;
    private final Long expectedVersion;

    public OrderStatusUpdatedEvent(UUID orderId, OrderStatus previousStatus, OrderStatus newStatus, Long expectedVersion) {
        super(orderId, "ORDER_STATUS_UPDATED");
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.expectedVersion = expectedVersion;
    }

    public OrderStatus getPreviousStatus() {
        return previousStatus;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

}