package com.trackops.server.domain.events.orders;

import java.util.UUID;

public class OrderDeliveredEvent extends OrderEvent {

    private final String orderDeliveredAt;

    public OrderDeliveredEvent(UUID orderId, String orderDeliveredAt) {

        super(orderId, "ORDER_DELIVERED");
        this.orderDeliveredAt = orderDeliveredAt;

    }

    public String getOrderDeliveredAt() {

        return orderDeliveredAt;

    }

}