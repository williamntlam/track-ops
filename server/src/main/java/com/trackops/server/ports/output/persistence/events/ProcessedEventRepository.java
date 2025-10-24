package com.trackops.server.ports.output.persistence.events;

import com.trackops.server.domain.model.events.ProcessedEvent;
import java.util.Optional;

public interface ProcessedEventRepository {

    ProcessedEvent save(ProcessedEvent processedEvent);
    Optional<ProcessedEvent> findByEventId(String eventId);
    Optional<ProcessedEvent> findByOrderId(Long orderId);
    boolean existsByEventId(String eventId);
    void deleteByEventId(String eventId);
}