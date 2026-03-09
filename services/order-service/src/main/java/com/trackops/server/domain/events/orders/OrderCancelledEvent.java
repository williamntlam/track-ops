package com.trackops.server.domain.events.orders;

import java.time.LocalDateTime;
import java.util.UUID;

public class OrderCancelledEvent extends OrderEvent {

    private final String cancelledBy;        // Who cancelled it
    private final String cancellationReason; // Why it was cancelled
    private final LocalDateTime cancelledAt; // When it was cancelled

    public OrderCancelledEvent(UUID orderId, String cancelledBy, String cancellationReason) {
        super(orderId, "ORDER_CANCELLED");
        this.cancelledBy = cancelledBy;
        this.cancellationReason = cancellationReason;
        this.cancelledAt = LocalDateTime.now();
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}