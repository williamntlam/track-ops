package com.trackops.server.adapters.output.persistence.dlq;

import com.trackops.server.domain.model.dlq.DlqOrder;
import com.trackops.server.ports.output.persistence.dlq.DlqOrderRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DlqOrderRepositoryAdapter implements DlqOrderRepository {

    private final DlqOrderJpaRepository jpaRepository;

    public DlqOrderRepositoryAdapter(DlqOrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DlqOrder save(DlqOrder dlqOrder) {
        return jpaRepository.save(dlqOrder);
    }

    @Override
    public Optional<DlqOrder> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<DlqOrder> findByOrderId(String orderId) {
        return jpaRepository.findByOrderId(orderId);
    }

    @Override
    public List<DlqOrder> findByStatus(String status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public List<DlqOrder> findPendingEligibleForRetry(LocalDateTime now) {
        return jpaRepository.findPendingEligibleForRetry(now);
    }

    @Override
    public long countByStatus(String status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public long countPending() {
        return jpaRepository.countPending();
    }
}
