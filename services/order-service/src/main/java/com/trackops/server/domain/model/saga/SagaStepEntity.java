package com.trackops.server.domain.model.saga;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga_steps")
public class SagaStepEntity {
    
    @Id
    @GeneratedValue(generator = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;
    
    @Column(name = "step_name", nullable = false)
    private String stepName;
    
    @Column(name = "service_name", nullable = false)
    private String serviceName;
    
    @Column(name = "action", nullable = false)
    private String action;
    
    @Column(name = "compensation_action", nullable = false)
    private String compensationAction;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStepStatus status;
    
    @Column(name = "started_at")
    private Instant startedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "retry_count")
    private int retryCount;
    
    @Column(name = "step_data", columnDefinition = "TEXT")
    private String stepData;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saga_instance_id", nullable = false)
    private SagaInstance sagaInstance;

    // Constructors
    public SagaStepEntity() {}

    public SagaStepEntity(UUID id, String stepName, String serviceName, String action,
                         String compensationAction, SagaStepStatus status, Instant startedAt,
                         Instant completedAt, String errorMessage, int retryCount, 
                         Object stepData, SagaInstance sagaInstance) {
        this.id = id;
        this.stepName = stepName;
        this.serviceName = serviceName;
        this.action = action;
        this.compensationAction = compensationAction;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.stepData = stepData != null ? stepData.toString() : null;
        this.sagaInstance = sagaInstance;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getCompensationAction() { return compensationAction; }
    public void setCompensationAction(String compensationAction) { this.compensationAction = compensationAction; }
    
    public SagaStepStatus getStatus() { return status; }
    public void setStatus(SagaStepStatus status) { this.status = status; }
    
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    
    public String getStepData() { return stepData; }
    public void setStepData(String stepData) { this.stepData = stepData; }
    
    public SagaInstance getSagaInstance() { return sagaInstance; }
    public void setSagaInstance(SagaInstance sagaInstance) { this.sagaInstance = sagaInstance; }
}
