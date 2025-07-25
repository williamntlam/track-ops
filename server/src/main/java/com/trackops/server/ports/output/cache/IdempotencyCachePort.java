package com.trackops.server.ports.output.cache;

import java.time.Duration;
import java.util.UUID;

public interface IdempotencyCachePort {

    boolean isEventProcessed(UUID eventId);
    void markEventProcessed(UUID eventId, Duration ttl);
    String getProcessingResult(UUID eventId);

}