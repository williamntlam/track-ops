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
    public boolean isEventProcessed(UUID eventId, String consumerGroup) {
        try {
            String key = "processed_event:" + eventId + ":" + consumerGroup;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Failed to check if event {} is processed for consumer group {}", eventId, consumerGroup, e);
            return false;
        }
    }

    @Override
    public void markEventProcessed(UUID eventId, String consumerGroup, Duration ttl) {
        try {
            String key = "processed_event:" + eventId + ":" + consumerGroup;
            redisTemplate.opsForValue().set(key, "true", ttl);
        } catch (Exception e) {
            log.error("Failed to mark event {} as processed for consumer group {}", eventId, consumerGroup, e);
        }
    }

    @Override
    public String getProcessingResult(UUID eventId, String consumerGroup) {
        try {
            String key = "processed_event:" + eventId + ":" + consumerGroup;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get processing result for event {} in consumer group {}", eventId, consumerGroup, e);
            return null;
        }
    }

}