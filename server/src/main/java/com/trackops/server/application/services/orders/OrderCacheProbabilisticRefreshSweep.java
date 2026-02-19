package com.trackops.server.application.services.orders;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Sweep-based probabilistic early revalidation: periodically scans cached order keys,
 * and for entries in the refresh window (near expiry), probabilistically triggers a
 * background refresh. Spreads revalidation load across the whole cache for scalability.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.cache.probabilistic-refresh.sweep-enabled", havingValue = "true")
@RequiredArgsConstructor
public class OrderCacheProbabilisticRefreshSweep {

    private static final String ORDER_ENTITY_KEY_PREFIX = "order:entity:";
    private static final String KEY_PATTERN = ORDER_ENTITY_KEY_PREFIX + "*";

    private final RedisTemplate<String, String> redisTemplate;
    private final OrderCacheBackgroundRefresher backgroundRefresher;

    @Value("${app.cache.probabilistic-refresh.window-ratio:0.2}")
    private double windowRatio;
    @Value("${app.cache.ttl.order:3600}")
    private long expectedOrderTtlSeconds;
    @Value("${app.cache.probabilistic-refresh.sweep-max-keys-per-run:500}")
    private int maxKeysPerRun;

    @Scheduled(fixedRateString = "${app.cache.probabilistic-refresh.sweep-interval-ms:300000}")
    public void sweep() {
        long windowSeconds = (long) (windowRatio * expectedOrderTtlSeconds);
        if (windowSeconds <= 0) return;

        Set<String> keys;
        try {
            keys = redisTemplate.keys(KEY_PATTERN);
        } catch (Exception e) {
            log.warn("Probabilistic refresh sweep failed to scan keys: {}", e.getMessage());
            return;
        }
        if (keys == null || keys.isEmpty()) return;

        List<String> toProcess = keys.stream()
                .limit(maxKeysPerRun)
                .toList();

        int refreshed = 0;
        for (String key : toProcess) {
            try {
                UUID orderId = orderIdFromKey(key);
                if (orderId == null) continue;

                Long remainingSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                if (remainingSeconds == null || remainingSeconds < 0) continue; // -2 missing, -1 no expiry

                if (remainingSeconds >= windowSeconds) continue; // not in refresh window

                double probability = 1.0 - (remainingSeconds / (double) windowSeconds);
                if (ThreadLocalRandom.current().nextDouble() >= probability) continue;

                backgroundRefresher.refreshOrderAsync(orderId);
                refreshed++;
            } catch (Exception e) {
                log.debug("Sweep skip key {}: {}", key, e.getMessage());
            }
        }

        if (refreshed > 0) {
            log.debug("Probabilistic refresh sweep completed: {} keys considered, {} refreshed", toProcess.size(), refreshed);
        }
    }

    private static UUID orderIdFromKey(String key) {
        if (key == null || !key.startsWith(ORDER_ENTITY_KEY_PREFIX)) return null;
        String uuidPart = key.substring(ORDER_ENTITY_KEY_PREFIX.length());
        try {
            return UUID.fromString(uuidPart);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
