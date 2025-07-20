package com.trackops.server.ports.output.cache;

import java.time.Duration;
import java.util.UUID;

public interface IdempotencyCachePort {

    boolean isEventProcessed(UUID eventId, String consumerGroup);
    void markEventProcessed(UUID eventId, String consumerGroup, Duration ttl);
    String getProcessingResult(UUID eventId, String consumerGroup);

}