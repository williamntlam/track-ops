package com.trackops.server.adapters.output.persistence.events;

import com.trackops.server.domain.model.events.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEvent, UUID> {

    Optional<ProcessedEvent> findByEventId(UUID eventId);

    List<ProcessedEvent> findByOrderId(UUID orderId);

}
