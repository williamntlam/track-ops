package com.trackops.server.adapters.output.persistence.outbox;

import com.trackops.server.domain.model.outbox.InventoryReserveOutboxEntry;
import com.trackops.server.ports.output.persistence.outbox.InventoryReserveOutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InventoryReserveOutboxRepositoryAdapter implements InventoryReserveOutboxRepository {

    private final InventoryReserveOutboxJpaRepository jpaRepository;

    public InventoryReserveOutboxRepositoryAdapter(InventoryReserveOutboxJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public InventoryReserveOutboxEntry save(InventoryReserveOutboxEntry entry) {
        return jpaRepository.save(entry);
    }

    @Override
    public boolean enqueueIfAbsent(UUID orderId) {
        int inserted = jpaRepository.insertOnConflictDoNothing(orderId);
        return inserted > 0;
    }

    @Override
    public Optional<InventoryReserveOutboxEntry> findByOrderId(UUID orderId) {
        return jpaRepository.findByOrderId(orderId);
    }

    @Override
    public List<InventoryReserveOutboxEntry> findPending(int maxRetries, int limit) {
        return jpaRepository.findPendingWithRetryLimit(maxRetries, PageRequest.of(0, limit));
    }
}
