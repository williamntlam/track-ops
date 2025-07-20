package com.trackops.server.application.services.events;

import org.springframework.stereotype.Service;
import com.trackops.server.ports.input.events.OrderEventProcessorPort;
import com.trackops.server.domain.events.orders.OrderEvent;
import com.trackops.server.ports.output.persistence.orders.OrderRepository;
import com.trackops.server.ports.output.persistence.events.ProcessedEventRepository;
import com.trackops.server.ports.output.cache.IdempotencyCachePort; 

@Service
public class OrderEventProcessorService implements OrderEventProcessorPort {

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final IdempotencyCachePort idempotencyCachePort;

    public OrderEventProcessorService(OrderRepository orderRepository, ProcessedEventRepository processedEventRepository, IdempotencyCachePort idempotencyCachePort) {
        
        this.orderRepository = orderRepository;
        this.processedEventRepository = processedEventRepository;
        this.idempotencyCachePort = idempotencyCachePort;
        
    }

    public void processOrderEvent(OrderEvent event) {
        
    }

}