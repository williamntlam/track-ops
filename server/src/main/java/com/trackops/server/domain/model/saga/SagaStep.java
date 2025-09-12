package com.trackops.server.domain.model.saga;

import java.time.Instant;
import java.util.UUID;

public class SagaStep {
    private final UUID id;
    private final String stepName;
    private final String serviceName;
    private final String action;
    private final String compensationAction;
    private final SagaStepStatus status;
    private final Instant startedAt;
    private final Instant completedAt;
    private final String errorMessage;
    private final int retryCount;
    private final Object stepData;

    public SagaStep(UUID id, String stepName, String serviceName, String action, 
                   String compensationAction, SagaStepStatus status, Instant startedAt, 
                   Instant completedAt, String errorMessage, int retryCount, Object stepData) {
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
        this.stepData = stepData;
    }

    public static SagaStep create(String stepName, String serviceName, String action, 
                                String compensationAction, Object stepData) {
        return new SagaStep(
            UUID.randomUUID(),
            stepName,
            serviceName,
            action,
            compensationAction,
            SagaStepStatus.PENDING,
            null,
            null,
            null,
            0,
            stepData
        );
    }

    public SagaStep markInProgress() {
        return new SagaStep(
            this.id,
            this.stepName,
            this.serviceName,
            this.action,
            this.compensationAction,
            SagaStepStatus.IN_PROGRESS,
            Instant.now(),
            this.completedAt,
            this.errorMessage,
            this.retryCount,
            this.stepData
        );
    }

    public SagaStep markCompleted() {
        return new SagaStep(
            this.id,
            this.stepName,
            this.serviceName,
            this.action,
            this.compensationAction,
            SagaStepStatus.COMPLETED,
            this.startedAt,
            Instant.now(),
            this.errorMessage,
            this.retryCount,
            this.stepData
        );
    }

    public SagaStep markFailed(String errorMessage) {
        return new SagaStep(
            this.id,
            this.stepName,
            this.serviceName,
            this.action,
            this.compensationAction,
            SagaStepStatus.FAILED,
            this.startedAt,
            this.completedAt,
            errorMessage,
            this.retryCount + 1,
            this.stepData
        );
    }

    public SagaStep markCompensated() {
        return new SagaStep(
            this.id,
            this.stepName,
            this.serviceName,
            this.action,
            this.compensationAction,
            SagaStepStatus.COMPENSATED,
            this.startedAt,
            Instant.now(),
            this.errorMessage,
            this.retryCount,
            this.stepData
        );
    }

    // Getters
    public UUID getId() { return id; }
    public String getStepName() { return stepName; }
    public String getServiceName() { return serviceName; }
    public String getAction() { return action; }
    public String getCompensationAction() { return compensationAction; }
    public SagaStepStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getErrorMessage() { return errorMessage; }
    public int getRetryCount() { return retryCount; }
    public Object getStepData() { return stepData; }
}
