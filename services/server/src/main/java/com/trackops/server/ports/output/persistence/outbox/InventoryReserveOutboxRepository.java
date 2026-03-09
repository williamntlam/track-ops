package com.trackops.server.ports.output.persistence.outbox;

import com.trackops.server.domain.model.outbox.InventoryReserveOutboxEntry;

import java.util.List;
import java.util.Optional;

public interface InventoryReserveOutboxRepository {

    /**
     * Save (insert or update) an outbox entry. Use ON CONFLICT DO NOTHING for idempotent enqueue per order.
     */
    InventoryReserveOutboxEntry save(InventoryReserveOutboxEntry entry);

    /**
     * Idempotent enqueue: insert a PENDING row for orderId. If order_id already exists, do nothing.
     * @return true if a new row was inserted, false if already present (idempotent)
     */
    boolean enqueueIfAbsent(java.util.UUID orderId);

    Optional<InventoryReserveOutboxEntry> findByOrderId(java.util.UUID orderId);

    /**
     * Find PENDING entries with retry_count < maxRetries, ordered by created_at, limit size.
     */
    List<InventoryReserveOutboxEntry> findPending(int maxRetries, int limit);
}
