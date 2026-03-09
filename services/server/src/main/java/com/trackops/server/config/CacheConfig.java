package com.trackops.server.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Creates a RedisCacheManager bean for Spring Cache abstraction.
     * This enables the cache actuator endpoint and provides cache statistics.
     * 
     * Cache names and TTLs are configured based on application.properties:
     * - orders: 3600 seconds (1 hour)
     * - status: 1800 seconds (30 minutes)
     * - response: 1800 seconds (30 minutes)
     * - customer: 3600 seconds (1 hour)
     * - page: 900 seconds (15 minutes)
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        System.out.println("INFO: Creating CacheManager bean for Spring Boot Actuator cache endpoint...");
        // Verify Redis connection is available
        try (RedisConnection connection = connectionFactory.getConnection()) {
            connection.ping();
            System.out.println("INFO: Redis connection verified successfully during CacheManager initialization");
        } catch (Exception e) {
            // Log the error but don't fail startup - cache will work once Redis is available
            // The actuator endpoint will handle the error gracefully
            System.err.println("Warning: Redis connection test failed during CacheManager initialization: " + e.getMessage());
        }
        
        // Default cache configuration with 1 hour TTL
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues();

        // Cache-specific configurations with different TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Order entities cache: 1 hour (3600 seconds)
        cacheConfigurations.put("orders", 
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(3600))
                        .disableCachingNullValues());
        
        // Order status cache: 30 minutes (1800 seconds)
        cacheConfigurations.put("status", 
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(1800))
                        .disableCachingNullValues());
        
        // API response cache: 30 minutes (1800 seconds)
        cacheConfigurations.put("response", 
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(1800))
                        .disableCachingNullValues());
        
        // Customer orders cache: 1 hour (3600 seconds)
        cacheConfigurations.put("customer", 
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(3600))
                        .disableCachingNullValues());
        
        // Paginated results cache: 15 minutes (900 seconds)
        cacheConfigurations.put("page", 
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(900))
                        .disableCachingNullValues());

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}

