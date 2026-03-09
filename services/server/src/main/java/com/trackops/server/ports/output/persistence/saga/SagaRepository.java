package com.trackops.server.ports.output.persistence.saga;

import com.trackops.server.domain.model.saga.SagaInstance;
import com.trackops.server.domain.model.saga.SagaStatus;
import com.trackops.server.domain.model.saga.SagaType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SagaRepository {
    
    SagaInstance save(SagaInstance sagaInstance);
    
    Optional<SagaInstance> findById(UUID id);
    
    List<SagaInstance> findByAggregateId(String aggregateId);
    
    List<SagaInstance> findBySagaType(SagaType sagaType);
    
    List<SagaInstance> findByStatus(SagaStatus status);
    
    List<SagaInstance> findIncompleteSagas();
    
    void deleteById(UUID id);
    
    boolean existsById(UUID id);
}
