package com.trackops.server.ports.output.events.orders;

import com.trackops.server.domain.events.orders.OrderCreatedEvent;
import com.trackops.server.domain.events.orders.OrderStatusUpdatedEvent;
import com.trackops.server.domain.events.orders.OrderDeliveredEvent;
import com.trackops.server.domain.events.orders.OrderCancelledEvent;

public interface OrderEventProducer {

    void publishOrderCreated(OrderCreatedEvent event);

    void publishOrderStatusUpdated(OrderStatusUpdatedEvent event);

    void publishOrderDelivered(OrderDeliveredEvent event);

    void publishOrderCancelled(OrderCancelledEvent event);

}