package com.trackops.server.ports.output.cache;

import com.trackops.server.domain.model.CacheOperationResult;
import java.time.Duration;
import java.util.UUID;

public interface IdempotencyCachePort {

    boolean isEventProcessed(UUID eventId);
    CacheOperationResult markEventProcessed(UUID eventId, Duration ttl);
    String getProcessingResult(UUID eventId);

}