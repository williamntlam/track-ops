package com.trackops.server.adapters.output.persistence.saga;

import com.trackops.server.domain.model.saga.SagaInstance;
import com.trackops.server.domain.model.saga.SagaStatus;
import com.trackops.server.domain.model.saga.SagaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SagaJpaRepository extends JpaRepository<SagaInstance, UUID> {
    
    List<SagaInstance> findByAggregateId(String aggregateId);
    
    List<SagaInstance> findBySagaType(SagaType sagaType);
    
    List<SagaInstance> findByStatus(SagaStatus status);
    
    @Query("SELECT s FROM SagaInstance s WHERE s.status IN ('STARTED', 'IN_PROGRESS', 'COMPENSATING')")
    List<SagaInstance> findIncompleteSagas();
}
