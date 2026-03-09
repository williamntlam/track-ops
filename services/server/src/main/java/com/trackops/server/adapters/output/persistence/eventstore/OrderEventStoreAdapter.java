package com.trackops.server.adapters.output.persistence.eventstore;

import com.trackops.server.domain.model.eventstore.OrderEvent;
import com.trackops.server.ports.output.persistence.eventstore.OrderEventStore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Event store implementation using Spring Data JPA.
 * No raw JDBC — uses the same connection pool and transaction manager as the rest of the app.
 */
@Repository
public class OrderEventStoreAdapter implements OrderEventStore {

    private final OrderEventJpaRepository jpaRepository;

    public OrderEventStoreAdapter(OrderEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public OrderEvent append(UUID orderId, String eventType, String payloadJson, int schemaVersion) {
        int nextSequence = jpaRepository.findMaxSequenceNumberByOrderId(orderId) + 1;
        OrderEvent event = OrderEvent.builder()
                .orderId(orderId)
                .eventType(eventType)
                .payload(payloadJson)
                .schemaVersion(schemaVersion)
                .sequenceNumber(nextSequence)
                .build();
        return jpaRepository.save(event);
    }

    @Override
    public List<OrderEvent> findByOrderIdOrderBySequenceNumberAsc(UUID orderId) {
        return jpaRepository.findByOrderIdOrderBySequenceNumberAsc(orderId);
    }
}
