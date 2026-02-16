package com.trackops.server.adapters.output.monitoring.scheduler;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Aspect that records start/end and duration for methods annotated with {@link CriticalScheduled},
 * so the scheduler health registry can detect stalled tasks.
 */
@Aspect
@Component
@ConditionalOnProperty(name = "app.scheduler.health.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerHealthAspect {

    @Value("${app.scheduler.health.default-max-staleness-ms:-1}")
    private long defaultMaxStalenessMs;

    private final SchedulerHealthRegistry registry;

    public SchedulerHealthAspect(SchedulerHealthRegistry registry) {
        this.registry = registry;
    }

    @Around("@annotation(criticalScheduled)")
    public Object recordScheduledExecution(ProceedingJoinPoint pjp, CriticalScheduled criticalScheduled) throws Throwable {
        String taskName = criticalScheduled.taskName();
        if (taskName == null || taskName.isEmpty()) {
            taskName = ((MethodSignature) pjp.getSignature()).getMethod().getName();
        }
        long maxStalenessMs = criticalScheduled.maxStalenessMs() > 0
                ? criticalScheduled.maxStalenessMs()
                : defaultMaxStalenessMs;

        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            registry.recordCompletion(taskName, durationMs, maxStalenessMs);
        }
    }
}
