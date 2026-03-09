package com.trackops.server.adapters.output.monitoring.scheduler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a scheduled method as critical for scheduler health monitoring.
 * Methods annotated with this (in addition to {@code @Scheduled}) will have their
 * last completion time and duration recorded. Health and metrics will alert when
 * no completion has occurred within the configured max-staleness threshold.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CriticalScheduled {

    /**
     * Task identifier for metrics and health. Defaults to the method name.
     */
    String taskName() default "";

    /**
     * Maximum allowed time (milliseconds) between successful completions.
     * If not set, the global default from {@code app.scheduler.health.default-max-staleness-ms} is used.
     * Recommended: 2× the scheduled interval (e.g. 120000 for a 60s task).
     */
    long maxStalenessMs() default -1;
}
