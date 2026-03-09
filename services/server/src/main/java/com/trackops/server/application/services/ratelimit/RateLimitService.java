package com.trackops.server.application.services.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import com.trackops.server.config.RateLimitConfig.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service for managing rate limiting using Bucket4j with Redis.
 * Provides distributed rate limiting across multiple service instances.
 */
@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties properties;

    public RateLimitService(
            ProxyManager<String> proxyManager,
            RateLimitProperties properties) {
        this.proxyManager = proxyManager;
        this.properties = properties;
    }

    /**
     * Checks if a request is allowed based on the client identifier.
     * 
     * @param clientId The client identifier (IP address, API key, etc.)
     * @return RateLimitResult containing whether the request is allowed and rate limit info
     */
    public RateLimitResult checkRateLimit(String clientId) {
        if (!properties.isEnabled()) {
            return RateLimitResult.allowed(properties.getDefaultRequestsPerHour(), 
                    properties.getDefaultRequestsPerHour(), 
                    System.currentTimeMillis() / 1000 + 3600);
        }

        try {
            String key = RATE_LIMIT_KEY_PREFIX + clientId;
            BucketConfiguration configuration = createBucketConfiguration();
            Bucket bucket = proxyManager.getProxy(key, () -> configuration);

            boolean allowed = bucket.tryConsume(1);
            long availableTokens = bucket.getAvailableTokens();
            long capacity = properties.getDefaultRequestsPerHour();
            long resetTime = System.currentTimeMillis() / 1000 + 3600; // Reset in 1 hour

            if (allowed) {
                logger.debug("Rate limit check passed for client: {}, remaining: {}", 
                        clientId, availableTokens);
                return RateLimitResult.allowed(capacity, availableTokens, resetTime);
            } else {
                logger.warn("Rate limit exceeded for client: {}, capacity: {}", 
                        clientId, capacity);
                return RateLimitResult.exceeded(capacity, 0, resetTime);
            }
        } catch (Exception e) {
            logger.error("Error checking rate limit for client: {}", clientId, e);
            // Fail open - allow request if rate limit check fails
            return RateLimitResult.allowed(properties.getDefaultRequestsPerHour(), 
                    properties.getDefaultRequestsPerHour(), 
                    System.currentTimeMillis() / 1000 + 3600);
        }
    }

    /**
     * Creates the bucket configuration with the default rate limit settings.
     */
    private BucketConfiguration createBucketConfiguration() {
        Bandwidth bandwidth = Bandwidth.classic(
                properties.getDefaultRequestsPerHour(),
                Refill.intervally(properties.getDefaultRequestsPerHour(), Duration.ofHours(1))
        ).withInitialTokens(properties.getBurstCapacity());
        
        return BucketConfiguration.builder()
                .addLimit(bandwidth)
                .build();
    }

    /**
     * Result of a rate limit check.
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final long limit;
        private final long remaining;
        private final long resetTime;

        private RateLimitResult(boolean allowed, long limit, long remaining, long resetTime) {
            this.allowed = allowed;
            this.limit = limit;
            this.remaining = remaining;
            this.resetTime = resetTime;
        }

        public static RateLimitResult allowed(long limit, long remaining, long resetTime) {
            return new RateLimitResult(true, limit, remaining, resetTime);
        }

        public static RateLimitResult exceeded(long limit, long remaining, long resetTime) {
            return new RateLimitResult(false, limit, remaining, resetTime);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getLimit() {
            return limit;
        }

        public long getRemaining() {
            return remaining;
        }

        public long getResetTime() {
            return resetTime;
        }
    }
}
