package com.trackops.server.domain.model.saga;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "saga_instances")
public class SagaInstance {
    
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaType sagaType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;
    
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId; // e.g., orderId
    
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "current_step_index")
    private int currentStepIndex;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "retry_count")
    private int retryCount;
    
    @Column(name = "max_retries")
    private int maxRetries;
    
    @OneToMany(mappedBy = "sagaInstance", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<SagaStepEntity> steps = new ArrayList<>();
    
    @Version
    private Long version;

    // Constructors
    public SagaInstance() {}

    public SagaInstance(SagaType sagaType, String aggregateId, int maxRetries) {
        this.sagaType = sagaType;
        this.aggregateId = aggregateId;
        this.status = SagaStatus.STARTED;
        this.startedAt = Instant.now();
        this.currentStepIndex = 0;
        this.retryCount = 0;
        this.maxRetries = maxRetries;
    }

    // Business methods
    public void addStep(SagaStep step) {
        SagaStepEntity stepEntity = new SagaStepEntity(
            step.getId(),
            step.getStepName(),
            step.getServiceName(),
            step.getAction(),
            step.getCompensationAction(),
            step.getStatus(),
            step.getStartedAt(),
            step.getCompletedAt(),
            step.getErrorMessage(),
            step.getRetryCount(),
            step.getStepData(),
            this
        );
        this.steps.add(stepEntity);
    }

    public void markStepInProgress(int stepIndex) {
        if (stepIndex < steps.size()) {
            SagaStepEntity step = steps.get(stepIndex);
            step.setStatus(SagaStepStatus.IN_PROGRESS);
            step.setStartedAt(Instant.now());
        }
    }

    public void markStepCompleted(int stepIndex) {
        if (stepIndex < steps.size()) {
            SagaStepEntity step = steps.get(stepIndex);
            step.setStatus(SagaStepStatus.COMPLETED);
            step.setCompletedAt(Instant.now());
            this.currentStepIndex = stepIndex + 1;
        }
    }

    public void markStepFailed(int stepIndex, String errorMessage) {
        if (stepIndex < steps.size()) {
            SagaStepEntity step = steps.get(stepIndex);
            step.setStatus(SagaStepStatus.FAILED);
            step.setErrorMessage(errorMessage);
            step.setRetryCount(step.getRetryCount() + 1);
            this.status = SagaStatus.FAILED;
            this.errorMessage = errorMessage;
        }
    }

    public void startCompensation() {
        this.status = SagaStatus.COMPENSATING;
    }

    public void markCompensated() {
        this.status = SagaStatus.COMPENSATED;
        this.completedAt = Instant.now();
    }

    public void markCompleted() {
        this.status = SagaStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetries;
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public boolean isCompleted() {
        return this.status == SagaStatus.COMPLETED || this.status == SagaStatus.COMPENSATED;
    }

    public boolean isFailed() {
        return this.status == SagaStatus.FAILED;
    }

    public boolean isCompensating() {
        return this.status == SagaStatus.COMPENSATING;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public SagaType getSagaType() { return sagaType; }
    public void setSagaType(SagaType sagaType) { this.sagaType = sagaType; }
    
    public SagaStatus getStatus() { return status; }
    public void setStatus(SagaStatus status) { this.status = status; }
    
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    
    public int getCurrentStepIndex() { return currentStepIndex; }
    public void setCurrentStepIndex(int currentStepIndex) { this.currentStepIndex = currentStepIndex; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    
    public List<SagaStepEntity> getSteps() { return steps; }
    public void setSteps(List<SagaStepEntity> steps) { this.steps = steps; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
