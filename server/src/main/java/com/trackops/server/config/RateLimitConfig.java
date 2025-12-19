package com.trackops.server.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringByteArrayCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for rate limiting using Bucket4j with Redis.
 * Provides distributed rate limiting across multiple service instances.
 */
@Configuration
public class RateLimitConfig {

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.rate-limit.default-requests-per-hour:1000}")
    private long defaultRequestsPerHour;

    @Value("${app.rate-limit.burst-capacity:100}")
    private long burstCapacity;

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    /**
     * Creates a Redis client for rate limiting.
     */
    @Bean(destroyMethod = "shutdown")
    public RedisClient rateLimitRedisClient() {
        RedisURI redisUri = RedisURI.builder()
                .withHost(redisHost)
                .withPort(redisPort)
                .withDatabase(redisDatabase)
                .build();

        return RedisClient.create(redisUri);
    }

    /**
     * Creates a Redis connection for rate limiting.
     * Uses byte[] codec for Bucket4j compatibility.
     */
    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> rateLimitRedisConnection(RedisClient redisClient) {
        return redisClient.connect(new StringByteArrayCodec());
    }

    /**
     * Creates a Redis-based proxy manager for distributed rate limiting.
     * This allows rate limits to be shared across multiple service instances.
     */
    @Bean
    public ProxyManager<String> rateLimitProxyManager(StatefulRedisConnection<String, byte[]> connection) {
        return LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(
                    io.github.bucket4j.distributed.ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                        Duration.ofHours(1)))
                .build();
    }

    /**
     * Creates a default rate limit bucket configuration.
     * Can be customized per client/endpoint as needed.
     */
    @Bean
    public RateLimitProperties rateLimitProperties() {
        return RateLimitProperties.builder()
                .enabled(rateLimitEnabled)
                .defaultRequestsPerHour(defaultRequestsPerHour)
                .burstCapacity(burstCapacity)
                .build();
    }

    /**
     * Configuration properties for rate limiting.
     */
    public static class RateLimitProperties {
        private boolean enabled;
        private long defaultRequestsPerHour;
        private long burstCapacity;

        public static RateLimitProperties builder() {
            return new RateLimitProperties();
        }

        public RateLimitProperties enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public RateLimitProperties defaultRequestsPerHour(long defaultRequestsPerHour) {
            this.defaultRequestsPerHour = defaultRequestsPerHour;
            return this;
        }

        public RateLimitProperties burstCapacity(long burstCapacity) {
            this.burstCapacity = burstCapacity;
            return this;
        }

        public RateLimitProperties build() {
            return this;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public long getDefaultRequestsPerHour() {
            return defaultRequestsPerHour;
        }

        public long getBurstCapacity() {
            return burstCapacity;
        }
    }
}
