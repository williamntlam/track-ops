package com.trackops.server.ports.output.events.orders;

import com.trackops.server.domain.events.orders.OrderCreatedEvent;
import com.trackops.server.domain.events.orders.OrderStatusUpdatedEvent;
import com.trackops.server.domain.events.orders.OrderDeliveredEvent;
import com.trackops.server.domain.events.orders.OrderCancelledEvent;

public interface OrderEventConsumer {

    void consumeOrderCreated(OrderCreatedEvent event);
    void consumeOrderStatusUpdated(OrderStatusUpdatedEvent event);
    void consumeOrderDelivered(OrderDeliveredEvent event);
    void consumeOrderCancelled(OrderCancelledEvent event);
    void processEvent(String eventJson, String topic);
    void handleEventProcessingError(Object event, Exception error);

}