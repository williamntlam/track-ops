package com.trackops.server.adapters.output.cache;

import com.trackops.server.ports.output.cache.OrderStatusCachePort;
import com.trackops.server.domain.model.CacheOperationResult;
import com.trackops.server.domain.model.enums.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.connection.SortParameters.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class RedisOrderStatusCacheAdapter implements OrderStatusCachePort {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisOrderStatusCacheAdapter.class);
    private final RedisTemplate<String, String> redisTemplate;

    public RedisOrderStatusCacheAdapter(RedisTemplate<String, String> redisTemplate) {

        this.redisTemplate = redisTemplate;

    }

    public CacheOperationResult cacheOrderStatus(UUID orderId, OrderStatus status, Duration ttl) {
        try {
            
            String key = getOrderStatusKey(orderId);
            String value = status.name();

            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {

                redisTemplate.opsForValue().set(key, value, ttl);

            }

            else {

                redisTemplate.opsForValue().set(key, value);

            }

            logger.debug("Successfully cached order status for orderId: {} with status: {}", orderId, status);
            return CacheOperationResult.success();
            
        } catch (Exception e) {
            logger.error("Failed to cache order status for orderId: {}", orderId, e);
            return CacheOperationResult.failure("Failed to cache order status: " + e.getMessage());
        }
    }

    public Optional<OrderStatus> getOrderStatus(UUID orderId) {
        try {
            String key = getOrderStatusKey(orderId);
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                logger.debug("Order status not found in cache for orderId: {}", orderId);
                return Optional.empty();
            }

            OrderStatus status = OrderStatus.valueOf(value);
            logger.debug("Retrieved order status from cache for orderId: {} with status: {}", orderId, status);
            return Optional.of(status);
        
        } catch (IllegalArgumentException e) {
            logger.error("Invalid order status value in cache for orderId: {} - value: {}", orderId, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to get order status for orderId: {}", orderId, e);
            return Optional.empty();
        }
    }

    public CacheOperationResult removeOrderStatus(UUID orderId) {
        try {
            
            String key = getOrderStatusKey(orderId);
            Boolean deleted = redisTemplate.delete(key);

            if (Boolean.TRUE.equals(deleted)) {
                logger.debug("Successfully removed order status from cache for orderId: {}", orderId);
                return CacheOperationResult.success();
            }

            logger.debug("Order status not found in cache for removal, orderId: {}", orderId);
            return CacheOperationResult.success();

        } catch (Exception e) {
            logger.error("Failed to remove order status for orderId: {}", orderId, e);
            return CacheOperationResult.failure("Failed to remove order status: " + e.getMessage());
        }
    }

    public boolean hasOrderStatus(UUID orderId) {
        try {
            
            String key = getOrderStatusKey(orderId);

            if (redisTemplate.hasKey(key)) {
                logger.debug("Order status exists in cache for orderId: {}", orderId);
                return true;
            } else {
                logger.debug("Order status not found in cache for orderId: {}", orderId);
                return false;
            }

        } catch (Exception e) {
            logger.error("Failed to check order status existence for orderId: {}", orderId, e);
            return false;
        }
    }

    public CacheOperationResult updateOrderStatus(UUID orderId, OrderStatus newStatus, Duration ttl) {
        try {

            String key = getOrderStatusKey(orderId);
            String value = newStatus.name();
            
            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                redisTemplate.opsForValue().set(key, value, ttl);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }

            logger.debug("Successfully updated order status in cache for orderId: {} with status: {}", orderId, newStatus);
            return CacheOperationResult.success();
        } catch (Exception e) {
            logger.error("Failed to update order status for orderId: {}", orderId, e);
            return CacheOperationResult.failure("Failed to update order status: " + e.getMessage());
        }
    }

    public String getOrderStatusKey(UUID orderId) {

        return "order:status:" + orderId.toString();

    }

}