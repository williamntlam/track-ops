package com.trackops.inventory.ports.input.events;

import com.trackops.inventory.domain.events.OrderEvent;

public interface OrderEventProcessorPort {
    void processOrderEvent(OrderEvent event);
}
