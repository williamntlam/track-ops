package com.trackops.server.ports.input.events;

import com.trackops.server.domain.events.orders.OrderEvent;

public interface OrderEventProcessorPort {

    public void processOrderEvent(OrderEvent event);

}