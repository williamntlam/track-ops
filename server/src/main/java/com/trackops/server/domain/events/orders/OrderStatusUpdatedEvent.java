package com.trackops.server.domain.events.orders;

public class OrderDeliveredEvent extends OrderEvent {

    private final String orderDeliveratedAt;

    public OrderDeliveredEvent(UUID orderId, String orderDeliveratedAt) {

        super(orderId, "orderDelivered")
        this.orderDeliveratedAt = orderDeliveratedAt;

    }

    public String getOrderDeliveratedAt() {

        return orderDeliveratedAt;

    }

}