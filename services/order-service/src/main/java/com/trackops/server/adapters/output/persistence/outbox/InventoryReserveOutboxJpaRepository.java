package com.trackops.server.adapters.output.persistence.outbox;

import com.trackops.server.domain.model.outbox.InventoryReserveOutboxEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryReserveOutboxJpaRepository extends JpaRepository<InventoryReserveOutboxEntry, UUID> {

    Optional<InventoryReserveOutboxEntry> findByOrderId(UUID orderId);

    @Query("SELECT e FROM InventoryReserveOutboxEntry e WHERE e.status = 'PENDING' AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    List<InventoryReserveOutboxEntry> findPendingWithRetryLimit(@Param("maxRetries") int maxRetries, org.springframework.data.domain.Pageable pageable);

    /**
     * Idempotent insert: only insert if no row exists for this order_id.
     * @return 1 if inserted, 0 if conflict (already enqueued)
     */
    @Modifying
    @Query(value = """
        INSERT INTO inventory_reserve_outbox (id, order_id, status, created_at, retry_count)
        VALUES (gen_random_uuid(), CAST(:orderId AS uuid), 'PENDING', CURRENT_TIMESTAMP, 0)
        ON CONFLICT (order_id) DO NOTHING
        """, nativeQuery = true)
    int insertOnConflictDoNothing(@Param("orderId") UUID orderId);
}
