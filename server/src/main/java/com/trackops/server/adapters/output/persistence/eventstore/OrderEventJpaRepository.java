package com.trackops.server.adapters.output.persistence.eventstore;

import com.trackops.server.domain.model.eventstore.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderEventJpaRepository extends JpaRepository<OrderEvent, Long> {

    List<OrderEvent> findByOrderIdOrderBySequenceNumberAsc(UUID orderId);

    @Query("SELECT COALESCE(MAX(e.sequenceNumber), 0) FROM OrderEvent e WHERE e.orderId = :orderId")
    int findMaxSequenceNumberByOrderId(@Param("orderId") UUID orderId);
}
