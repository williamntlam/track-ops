package com.trackops.server.application.services.events;

import org.springframework.stereotype.Service;
import com.trackops.server.ports.input.events.OrderEventProcessorPort;
import com.trackops.server.domain.events.orders.OrderEvent;
import com.trackops.server.ports.output.persistence.orders.OrderRepository;
import com.trackops.server.ports.output.persistence.events.ProcessedEventRepository;
import com.trackops.server.ports.output.cache.IdempotencyCachePort; 

@Service
public class OrderEventProcessorService implements OrderEventProcessorPort {

    // ports
    // order repository
    // processed event repository
    // idempotency cache

    public void processOrderEvent(OrderEvent event) {
        
    }

}