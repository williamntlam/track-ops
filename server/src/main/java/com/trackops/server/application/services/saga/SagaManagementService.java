package com.trackops.server.application.services.saga;

import com.trackops.server.domain.model.saga.SagaInstance;
import com.trackops.server.domain.model.saga.SagaStatus;
import com.trackops.server.ports.output.persistence.saga.SagaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SagaManagementService {
    
    private static final Logger log = LoggerFactory.getLogger(SagaManagementService.class);
    
    private final SagaRepository sagaRepository;
    private final SagaOrchestratorService sagaOrchestratorService;

    public SagaManagementService(SagaRepository sagaRepository, SagaOrchestratorService sagaOrchestratorService) {
        this.sagaRepository = sagaRepository;
        this.sagaOrchestratorService = sagaOrchestratorService;
    }

    /**
     * Scheduled task to recover incomplete SAGAs
     * Runs every 30 seconds
     */
    @Scheduled(fixedDelay = 30000)
    public void recoverIncompleteSagas() {
        try {
            List<SagaInstance> incompleteSagas = sagaRepository.findIncompleteSagas();
            
            if (!incompleteSagas.isEmpty()) {
                log.info("Found {} incomplete SAGAs, attempting recovery", incompleteSagas.size());
                
                for (SagaInstance saga : incompleteSagas) {
                    try {
                        log.info("Recovering SAGA: {} with status: {}", saga.getId(), saga.getStatus());
                        
                        if (saga.getStatus() == SagaStatus.FAILED && saga.canRetry()) {
                            saga.incrementRetry();
                            saga.setStatus(SagaStatus.STARTED);
                            saga.setCurrentStepIndex(0);
                            saga.setErrorMessage(null);
                            sagaRepository.save(saga);
                            
                            // Retry the SAGA
                            sagaOrchestratorService.executeSaga(saga.getId());
                        } else if (saga.getStatus() == SagaStatus.IN_PROGRESS) {
                            // Continue execution
                            sagaOrchestratorService.executeSaga(saga.getId());
                        }
                    } catch (Exception e) {
                        log.error("Error recovering SAGA {}: {}", saga.getId(), e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during SAGA recovery: {}", e.getMessage(), e);
        }
    }

    /**
     * Get SAGA status by ID
     */
    public SagaInstance getSagaStatus(String sagaId) {
        return sagaRepository.findById(java.util.UUID.fromString(sagaId)).orElse(null);
    }

    /**
     * Get all SAGAs for an order
     */
    public List<SagaInstance> getSagasForOrder(String orderId) {
        return sagaRepository.findByAggregateId(orderId);
    }

    /**
     * Get SAGAs by status
     */
    public List<SagaInstance> getSagasByStatus(SagaStatus status) {
        return sagaRepository.findByStatus(status);
    }

    /**
     * Manually retry a failed SAGA
     */
    public boolean retrySaga(String sagaId) {
        try {
            SagaInstance saga = sagaRepository.findById(java.util.UUID.fromString(sagaId))
                .orElseThrow(() -> new RuntimeException("SAGA not found: " + sagaId));
            
            if (saga.getStatus() != SagaStatus.FAILED) {
                log.warn("Cannot retry SAGA {} with status: {}", sagaId, saga.getStatus());
                return false;
            }
            
            if (!saga.canRetry()) {
                log.warn("SAGA {} has exceeded max retries", sagaId);
                return false;
            }
            
            saga.incrementRetry();
            saga.setStatus(SagaStatus.STARTED);
            saga.setCurrentStepIndex(0);
            saga.setErrorMessage(null);
            sagaRepository.save(saga);
            
            sagaOrchestratorService.executeSaga(saga.getId());
            return true;
        } catch (Exception e) {
            log.error("Error retrying SAGA {}: {}", sagaId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Manually compensate a SAGA
     */
    public boolean compensateSaga(String sagaId) {
        try {
            SagaInstance saga = sagaRepository.findById(java.util.UUID.fromString(sagaId))
                .orElseThrow(() -> new RuntimeException("SAGA not found: " + sagaId));
            
            if (saga.isCompleted()) {
                log.warn("Cannot compensate completed SAGA: {}", sagaId);
                return false;
            }
            
            sagaOrchestratorService.compensateSaga(saga.getId());
            return true;
        } catch (Exception e) {
            log.error("Error compensating SAGA {}: {}", sagaId, e.getMessage(), e);
            return false;
        }
    }
}
