package com.trackops.server.ports.output.events.orders;

import com.trackops.server.domain.model.OperationResult;
import com.trackops.server.domain.events.orders.OrderCreatedEvent;
import com.trackops.server.domain.events.orders.OrderStatusUpdatedEvent;
import com.trackops.server.domain.events.orders.OrderDeliveredEvent;
import com.trackops.server.domain.events.orders.OrderCancelledEvent;

public interface OrderEventProducer {

    OperationResult publishOrderCreated(OrderCreatedEvent event);

    OperationResult publishOrderStatusUpdated(OrderStatusUpdatedEvent event);

    OperationResult publishOrderDelivered(OrderDeliveredEvent event);

    OperationResult publishOrderCancelled(OrderCancelledEvent event);

}