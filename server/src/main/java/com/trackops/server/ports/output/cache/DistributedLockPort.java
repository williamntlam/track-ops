package com.trackops.server.ports.output.cache;

import java.time.Duration;
import java.util.UUID;

/**
 * Port for distributed locking to prevent cache stampeding (thundering herd).
 * When loading an order on cache miss, one thread acquires the lock and loads;
 * others wait and then re-check the cache.
 */
public interface DistributedLockPort {

    /**
     * Try to acquire the lock for loading the given order. Used to ensure only one thread
     * loads from DB and populates cache for a given orderId.
     *
     * @param orderId order identifier
     * @param waitTimeout max time to wait for the lock
     * @param leaseTime how long the lock is held once acquired
     * @return true if lock was acquired, false if wait timeout elapsed without acquiring
     */
    boolean tryLockForOrderLoad(UUID orderId, Duration waitTimeout, Duration leaseTime);

    /**
     * Release the lock for the given order. Must be called by the thread that acquired it.
     *
     * @param orderId order identifier
     */
    void unlockForOrderLoad(UUID orderId);
}
