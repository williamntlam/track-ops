package com.trackops.server.application.services.events;

import org.springframework.stereotype.Service;
import com.trackops.server.ports.input.events.OrderEventProcessorPort;
import com.trackops.server.domain.events.orders.OrderEvent;
import com.trackops.server.ports.output.persistence.orders.OrderRepository;
import com.trackops.server.ports.output.persistence.events.ProcessedEventRepository;
import com.trackops.server.ports.output.cache.IdempotencyCachePort; 

import java.util.UUID;

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
        
        try {

            // Step One: Check idempotency cache port to see if the 
            // database query has already been completed.

            UUID eventId = event.getEventId();
            UUID orderId = event.getOrderId();

            if (idempotencyCachePort.isEventProcessed(eventId, "consumer_group")) {

                

            }


            // Step Two: Load and Process the order with the OrderRepository

            // Step Three: Mark event as processed with ProcessedEventRepository


        } catch (error) {

        }

    }

}