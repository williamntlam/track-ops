package com.trackops.server.adapters.output.persistence.events;

import com.trackops.server.domain.model.events.ProcessedEvent;
import com.trackops.server.ports.output.persistence.events.ProcessedEventRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import com.trackops.server.adapters.output.persistence.events.ProcessedEventJpaRepository;

@Repository
public class ProcessedEventRepositoryAdapter implements ProcessedEventRepository {
    
    private final ProcessedEventJpaRepository processedEventJpaRepository;

    public ProcessedEventRepositoryAdapter(ProcessedEventJpaRepository processedEventJpaRepository) {
        this.processedEventJpaRepository = processedEventJpaRepository;
    }

    @Override
    public ProcessedEvent save(ProcessedEvent processedEvent) {
        return processedEventJpaRepository.save(processedEvent);
    }

    @Override
    public Optional<ProcessedEvent> findByEventId(UUID eventId) {
        return processedEventJpaRepository.findByEventId(eventId);
    }

    @Override
    public Optional<ProcessedEvent> findByOrderId(UUID orderId) {
        return processedEventJpaRepository.findByOrderId(orderId);
    }

    @Override
    public List<ProcessedEvent> findAll() {
        return processedEventJpaRepository.findAll();
    }

    @Override
    public void deleteById(UUID id) {
        processedEventJpaRepository.delete(id);
    }

    @Override
    public boolean existsById(UUID id) {
        returnprocessedEventJpaRepository.find(id);
    }

}