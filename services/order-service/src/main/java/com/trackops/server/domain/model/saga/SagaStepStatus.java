package com.trackops.server.domain.model.saga;

public enum SagaStepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    COMPENSATED
}
