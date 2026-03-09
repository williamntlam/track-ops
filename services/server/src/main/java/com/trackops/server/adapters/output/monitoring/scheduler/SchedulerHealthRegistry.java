package com.trackops.server.adapters.output.monitoring.scheduler;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe registry of last completion time and duration for critical scheduled tasks.
 * Used by the scheduler health indicator and metrics to detect stalled tasks.
 */
@Component
public class SchedulerHealthRegistry {

    private final Map<String, TaskRecord> tasks = new ConcurrentHashMap<>();

    /**
     * Default max staleness when not specified per-task (5 minutes).
     */
    public static final long DEFAULT_MAX_STALENESS_MS = 300_000;

    /**
     * Registers or updates a task's completion. Call this when a critical scheduled method finishes.
     *
     * @param taskName   unique task identifier
     * @param durationMs execution duration in milliseconds
     * @param maxStalenessMs max allowed gap between completions; use -1 for default
     */
    public void recordCompletion(String taskName, long durationMs, long maxStalenessMs) {
        long now = System.currentTimeMillis();
        tasks.compute(taskName, (k, existing) -> {
            long effectiveMaxStaleness = maxStalenessMs > 0 ? maxStalenessMs : DEFAULT_MAX_STALENESS_MS;
            if (existing == null) {
                return new TaskRecord(now, durationMs, effectiveMaxStaleness);
            }
            existing.lastCompletionTimeMs = now;
            existing.lastDurationMs = durationMs;
            if (maxStalenessMs > 0) {
                existing.maxStalenessMs = maxStalenessMs;
            }
            return existing;
        });
    }

    /**
     * Returns the last completion time (epoch ms) for a task, or 0 if never run.
     */
    public long getLastCompletionTimeMs(String taskName) {
        TaskRecord r = tasks.get(taskName);
        return r == null ? 0 : r.lastCompletionTimeMs;
    }

    /**
     * Returns the last run duration (ms) for a task, or 0 if never run.
     */
    public long getLastDurationMs(String taskName) {
        TaskRecord r = tasks.get(taskName);
        return r == null ? 0 : r.lastDurationMs;
    }

    /**
     * Returns the configured max staleness (ms) for a task.
     */
    public long getMaxStalenessMs(String taskName) {
        TaskRecord r = tasks.get(taskName);
        return r == null ? DEFAULT_MAX_STALENESS_MS : r.maxStalenessMs;
    }

    /**
     * Milliseconds since last completion. Returns Long.MAX_VALUE if task has never run.
     */
    public long getMillisSinceLastCompletion(String taskName) {
        long last = getLastCompletionTimeMs(taskName);
        if (last == 0) return Long.MAX_VALUE;
        return System.currentTimeMillis() - last;
    }

    /**
     * All registered task names (tasks that have completed at least once).
     */
    public Map<String, TaskRecord> getTaskRecords() {
        return Map.copyOf(tasks);
    }

    public static final class TaskRecord {
        private long lastCompletionTimeMs;
        private long lastDurationMs;
        private long maxStalenessMs;

        TaskRecord(long lastCompletionTimeMs, long lastDurationMs, long maxStalenessMs) {
            this.lastCompletionTimeMs = lastCompletionTimeMs;
            this.lastDurationMs = lastDurationMs;
            this.maxStalenessMs = maxStalenessMs;
        }

        public long getLastCompletionTimeMs() {
            return lastCompletionTimeMs;
        }

        public long getLastDurationMs() {
            return lastDurationMs;
        }

        public long getMaxStalenessMs() {
            return maxStalenessMs;
        }

        public long getMillisSinceLastCompletion() {
            if (lastCompletionTimeMs == 0) return Long.MAX_VALUE;
            return System.currentTimeMillis() - lastCompletionTimeMs;
        }
    }
}
