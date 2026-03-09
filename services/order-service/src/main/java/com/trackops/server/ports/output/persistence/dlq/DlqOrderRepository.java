package com.trackops.server.ports.output.persistence.dlq;

import com.trackops.server.domain.model.dlq.DlqOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DlqOrderRepository {

    DlqOrder save(DlqOrder dlqOrder);

    Optional<DlqOrder> findById(UUID id);

    List<DlqOrder> findByOrderId(String orderId);

    List<DlqOrder> findByStatus(String status);

    /**
     * Find PENDING records eligible for retry (next_retry_at in the past or null).
     */
    List<DlqOrder> findPendingEligibleForRetry(LocalDateTime now);

    long countByStatus(String status);

    long countPending();

    /** Total row count in dlq_orders (for throttle / disk protection). */
    long count();
}
