package com.trackops.server.adapters.output.cache;

import com.trackops.server.ports.output.cache.DistributedLockPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonDistributedLockAdapter implements DistributedLockPort {

    private static final String LOCK_KEY_PREFIX = "order:load:lock:";

    private final RedissonClient redissonClient;

    @Override
    public boolean tryLockForOrderLoad(UUID orderId, Duration waitTimeout, Duration leaseTime) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + orderId);
        try {
            return lock.tryLock(waitTimeout.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for order load lock: {}", orderId);
            return false;
        }
    }

    @Override
    public void unlockForOrderLoad(UUID orderId) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + orderId);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Released load lock for order: {}", orderId);
        }
    }
}
