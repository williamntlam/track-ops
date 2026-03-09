package com.trackops.server.adapters.output.persistence.saga;

import com.trackops.server.domain.model.saga.SagaInstance;
import com.trackops.server.domain.model.saga.SagaStatus;
import com.trackops.server.domain.model.saga.SagaType;
import com.trackops.server.ports.output.persistence.saga.SagaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SagaRepositoryAdapter implements SagaRepository {
    
    private final SagaJpaRepository sagaJpaRepository;

    public SagaRepositoryAdapter(SagaJpaRepository sagaJpaRepository) {
        this.sagaJpaRepository = sagaJpaRepository;
    }

    @Override
    public SagaInstance save(SagaInstance sagaInstance) {
        return sagaJpaRepository.save(sagaInstance);
    }

    @Override
    public Optional<SagaInstance> findById(UUID id) {
        return sagaJpaRepository.findById(id);
    }

    @Override
    public List<SagaInstance> findByAggregateId(String aggregateId) {
        return sagaJpaRepository.findByAggregateId(aggregateId);
    }

    @Override
    public List<SagaInstance> findBySagaType(SagaType sagaType) {
        return sagaJpaRepository.findBySagaType(sagaType);
    }

    @Override
    public List<SagaInstance> findByStatus(SagaStatus status) {
        return sagaJpaRepository.findByStatus(status);
    }

    @Override
    public List<SagaInstance> findIncompleteSagas() {
        return sagaJpaRepository.findIncompleteSagas();
    }

    @Override
    public void deleteById(UUID id) {
        sagaJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return sagaJpaRepository.existsById(id);
    }
}
