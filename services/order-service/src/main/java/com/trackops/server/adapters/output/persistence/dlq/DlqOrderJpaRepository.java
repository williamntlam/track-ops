package com.trackops.server.adapters.output.persistence.dlq;

import com.trackops.server.domain.model.dlq.DlqOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DlqOrderJpaRepository extends JpaRepository<DlqOrder, UUID> {

    List<DlqOrder> findByOrderId(String orderId);

    List<DlqOrder> findByStatus(String status);

    @Query("SELECT d FROM DlqOrder d WHERE d.status = 'PENDING' AND (d.nextRetryAt IS NULL OR d.nextRetryAt <= :now)")
    List<DlqOrder> findPendingEligibleForRetry(@Param("now") LocalDateTime now);

    long countByStatus(String status);

    @Query("SELECT COUNT(d) FROM DlqOrder d WHERE d.status = 'PENDING'")
    long countPending();
}
