package com.trackops.server.adapters.output.persistence.events;

import com.trackops.server.domain.model.events.ProcessedEvent;
import com.trackops.server.ports.output.persistence.events.ProcessedEventRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import com.trackops.server.adapters.output.persistence.events.ProcessedEventJpaRepository;

@Repository
public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEvent, UUID> {

    public Optional<ProcessedEvent> findByEventId(UUID eventId);

    public List<ProcessedEvent> findByOrderId(UUID orderId);

}