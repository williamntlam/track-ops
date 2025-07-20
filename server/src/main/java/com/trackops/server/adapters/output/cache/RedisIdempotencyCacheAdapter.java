package com.trackops.server.adapters.output.cache;

import com.trackops.server.ports.output.cache.IdempotencyCachePort;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;
import java.time.Duration;

@Service
public class RedisIdempotencyCacheAdapter implements IdempotencyCachePort {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyCacheAdapter.class);
    private final RedisTemplate<String, String> redisTemplate;

    public RedisIdempotencyCacheAdapter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isEventProcessed(UUID eventId) {
        try {
            String key = "processed_event:" + eventId;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Failed to check if event {} is processed", eventId, e);
            return false;
        }
    }

    @Override
    public void markEventProcessed(UUID eventId, Duration ttl) {
        try {
            String key = "processed_event:" + eventId;
            redisTemplate.opsForValue().set(key, "true", ttl);
        } catch (Exception e) {
            log.error("Failed to mark event {} as processed", eventId, e);
        }
    }

    @Override
    public String getProcessingResult(UUID eventId) {
        try {
            String key = "processed_event:" + eventId;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get processing result for event {}", eventId, e);
            return null;
        }
    }

}