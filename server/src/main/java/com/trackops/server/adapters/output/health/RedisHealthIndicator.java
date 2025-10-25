package com.trackops.server.adapters.output.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Custom health indicator for Redis connectivity and performance.
 * Provides detailed information about Redis health including connection status,
 * memory usage, and cache statistics.
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisHealthIndicator.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    public RedisHealthIndicator(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public Health health() {
        try {
            // Test basic connectivity with ping
            String pong = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();
            
            if (!"PONG".equals(pong)) {
                return Health.down()
                    .withDetail("redis", "Redis")
                    .withDetail("error", "Ping failed")
                    .withDetail("response", pong)
                    .build();
            }
            
            // Test write/read operations
            long startTime = System.currentTimeMillis();
            String testKey = "health:check:" + System.currentTimeMillis();
            String testValue = "health-check-value";
            
            redisTemplate.opsForValue().set(testKey, testValue);
            String retrievedValue = redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);
            
            long operationTime = System.currentTimeMillis() - startTime;
            
            if (!testValue.equals(retrievedValue)) {
                return Health.down()
                    .withDetail("redis", "Redis")
                    .withDetail("error", "Read/write test failed")
                    .withDetail("expected", testValue)
                    .withDetail("actual", retrievedValue)
                    .build();
            }
            
            // Get Redis server information
            Properties info = redisTemplate.getConnectionFactory()
                .getConnection()
                .info();
            
            String version = info.getProperty("redis_version");
            String usedMemory = info.getProperty("used_memory_human");
            String connectedClients = info.getProperty("connected_clients");
            String uptime = info.getProperty("uptime_in_seconds");
            
            // Count cache keys
            long cacheKeyCount = redisTemplate.keys("order:*").size();
            
            Health.Builder healthBuilder = Health.up()
                .withDetail("redis", "Redis")
                .withDetail("version", version)
                .withDetail("operationTime", operationTime + "ms")
                .withDetail("usedMemory", usedMemory)
                .withDetail("connectedClients", connectedClients)
                .withDetail("uptime", uptime + "s")
                .withDetail("cacheKeyCount", cacheKeyCount)
                .withDetail("status", "Connected and responsive");
            
            // Add performance warnings
            if (operationTime > 100) {
                healthBuilder = healthBuilder.withDetail("warning", "Slow Redis operations detected");
            }
            
            if (operationTime > 1000) {
                return Health.down()
                    .withDetail("redis", "Redis")
                    .withDetail("error", "Redis operation timeout")
                    .withDetail("operationTime", operationTime + "ms")
                    .build();
            }
            
            logger.debug("Redis health check completed successfully in {}ms", operationTime);
            return healthBuilder.build();
            
        } catch (Exception e) {
            logger.error("Redis health check failed", e);
            return Health.down()
                .withDetail("redis", "Redis")
                .withDetail("error", e.getMessage())
                .withDetail("status", "Connection failed")
                .build();
        }
    }
}
