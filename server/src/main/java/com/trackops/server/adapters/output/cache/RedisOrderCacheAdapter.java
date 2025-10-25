package com.trackops.server.adapters.output.cache;

import com.trackops.server.ports.output.cache.OrderCachePort;
import com.trackops.server.domain.model.CacheOperationResult;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.adapters.input.web.dto.OrderResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RedisOrderCacheAdapter implements OrderCachePort {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisOrderCacheAdapter.class);
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisOrderCacheAdapter(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // Single Order Operations
    @Override
    public CacheOperationResult cacheOrder(Order order, Duration ttl) {
        try {
            String key = getOrderKey(order.getId());
            String value = objectMapper.writeValueAsString(order);
            
            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                redisTemplate.opsForValue().set(key, value, ttl);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
            
            logger.debug("Successfully cached order: {}", order.getId());
            return CacheOperationResult.success();
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize order {}: {}", order.getId(), e.getMessage());
            return CacheOperationResult.failure("Failed to serialize order: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to cache order {}: {}", order.getId(), e.getMessage());
            return CacheOperationResult.failure("Failed to cache order: " + e.getMessage());
        }
    }

    @Override
    public Optional<Order> getOrder(UUID orderId) {
        try {
            String key = getOrderKey(orderId);
            String value = redisTemplate.opsForValue().get(key);
            
            if (value != null) {
                Order order = objectMapper.readValue(value, Order.class);
                logger.debug("Cache hit for order: {}", orderId);
                return Optional.of(order);
            } else {
                logger.debug("Cache miss for order: {}", orderId);
                return Optional.empty();
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize order {}: {}", orderId, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to get order from cache {}: {}", orderId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public CacheOperationResult removeOrder(UUID orderId) {
        try {
            String key = getOrderKey(orderId);
            Boolean deleted = redisTemplate.delete(key);
            
            if (Boolean.TRUE.equals(deleted)) {
                logger.debug("Successfully removed order from cache: {}", orderId);
                return CacheOperationResult.success();
            } else {
                logger.debug("Order not found in cache for removal: {}", orderId);
                return CacheOperationResult.success(); // Not an error if not found
            }
        } catch (Exception e) {
            logger.error("Failed to remove order from cache {}: {}", orderId, e.getMessage());
            return CacheOperationResult.failure("Failed to remove order: " + e.getMessage());
        }
    }

    @Override
    public boolean hasOrder(UUID orderId) {
        try {
            String key = getOrderKey(orderId);
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            logger.error("Failed to check order existence in cache {}: {}", orderId, e.getMessage());
            return false;
        }
    }

    @Override
    public CacheOperationResult updateOrder(Order order, Duration ttl) {
        return cacheOrder(order, ttl); // Same as cache for updates
    }

    // Order Response Operations
    @Override
    public CacheOperationResult cacheOrderResponse(UUID orderId, OrderResponse response, Duration ttl) {
        try {
            String key = getOrderResponseKey(orderId);
            String value = objectMapper.writeValueAsString(response);
            
            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                redisTemplate.opsForValue().set(key, value, ttl);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
            
            logger.debug("Successfully cached order response: {}", orderId);
            return CacheOperationResult.success();
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize order response {}: {}", orderId, e.getMessage());
            return CacheOperationResult.failure("Failed to serialize order response: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to cache order response {}: {}", orderId, e.getMessage());
            return CacheOperationResult.failure("Failed to cache order response: " + e.getMessage());
        }
    }

    @Override
    public Optional<OrderResponse> getOrderResponse(UUID orderId) {
        try {
            String key = getOrderResponseKey(orderId);
            String value = redisTemplate.opsForValue().get(key);
            
            if (value != null) {
                OrderResponse response = objectMapper.readValue(value, OrderResponse.class);
                logger.debug("Cache hit for order response: {}", orderId);
                return Optional.of(response);
            } else {
                logger.debug("Cache miss for order response: {}", orderId);
                return Optional.empty();
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize order response {}: {}", orderId, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to get order response from cache {}: {}", orderId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public CacheOperationResult removeOrderResponse(UUID orderId) {
        try {
            String key = getOrderResponseKey(orderId);
            Boolean deleted = redisTemplate.delete(key);
            
            if (Boolean.TRUE.equals(deleted)) {
                logger.debug("Successfully removed order response from cache: {}", orderId);
                return CacheOperationResult.success();
            } else {
                logger.debug("Order response not found in cache for removal: {}", orderId);
                return CacheOperationResult.success();
            }
        } catch (Exception e) {
            logger.error("Failed to remove order response from cache {}: {}", orderId, e.getMessage());
            return CacheOperationResult.failure("Failed to remove order response: " + e.getMessage());
        }
    }

    @Override
    public CacheOperationResult updateOrderResponse(UUID orderId, OrderResponse response, Duration ttl) {
        return cacheOrderResponse(orderId, response, ttl);
    }

    // Bulk Operations
    @Override
    public CacheOperationResult cacheOrdersByStatus(String status, List<OrderResponse> orders, Duration ttl) {
        try {
            String key = getOrdersByStatusKey(status);
            String value = objectMapper.writeValueAsString(orders);
            
            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                redisTemplate.opsForValue().set(key, value, ttl);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
            
            logger.debug("Successfully cached {} orders by status: {}", orders.size(), status);
            return CacheOperationResult.success();
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize orders by status {}: {}", status, e.getMessage());
            return CacheOperationResult.failure("Failed to serialize orders: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to cache orders by status {}: {}", status, e.getMessage());
            return CacheOperationResult.failure("Failed to cache orders: " + e.getMessage());
        }
    }

    @Override
    public Optional<List<OrderResponse>> getOrdersByStatus(String status) {
        try {
            String key = getOrdersByStatusKey(status);
            String value = redisTemplate.opsForValue().get(key);
            
            if (value != null) {
                List<OrderResponse> orders = objectMapper.readValue(value, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OrderResponse.class));
                logger.debug("Cache hit for orders by status: {}", status);
                return Optional.of(orders);
            } else {
                logger.debug("Cache miss for orders by status: {}", status);
                return Optional.empty();
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize orders by status {}: {}", status, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to get orders by status from cache {}: {}", status, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public CacheOperationResult removeOrdersByStatus(String status) {
        try {
            String key = getOrdersByStatusKey(status);
            Boolean deleted = redisTemplate.delete(key);
            
            if (Boolean.TRUE.equals(deleted)) {
                logger.debug("Successfully removed orders by status from cache: {}", status);
                return CacheOperationResult.success();
            } else {
                logger.debug("Orders by status not found in cache for removal: {}", status);
                return CacheOperationResult.success();
            }
        } catch (Exception e) {
            logger.error("Failed to remove orders by status from cache {}: {}", status, e.getMessage());
            return CacheOperationResult.failure("Failed to remove orders by status: " + e.getMessage());
        }
    }

    @Override
    public CacheOperationResult cacheOrdersByCustomer(UUID customerId, List<OrderResponse> orders, Duration ttl) {
        try {
            String key = getOrdersByCustomerKey(customerId);
            String value = objectMapper.writeValueAsString(orders);
            
            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                redisTemplate.opsForValue().set(key, value, ttl);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
            
            logger.debug("Successfully cached {} orders by customer: {}", orders.size(), customerId);
            return CacheOperationResult.success();
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize orders by customer {}: {}", customerId, e.getMessage());
            return CacheOperationResult.failure("Failed to serialize orders: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to cache orders by customer {}: {}", customerId, e.getMessage());
            return CacheOperationResult.failure("Failed to cache orders: " + e.getMessage());
        }
    }

    @Override
    public Optional<List<OrderResponse>> getOrdersByCustomer(UUID customerId) {
        try {
            String key = getOrdersByCustomerKey(customerId);
            String value = redisTemplate.opsForValue().get(key);
            
            if (value != null) {
                List<OrderResponse> orders = objectMapper.readValue(value, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OrderResponse.class));
                logger.debug("Cache hit for orders by customer: {}", customerId);
                return Optional.of(orders);
            } else {
                logger.debug("Cache miss for orders by customer: {}", customerId);
                return Optional.empty();
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize orders by customer {}: {}", customerId, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to get orders by customer from cache {}: {}", customerId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public CacheOperationResult removeOrdersByCustomer(UUID customerId) {
        try {
            String key = getOrdersByCustomerKey(customerId);
            Boolean deleted = redisTemplate.delete(key);
            
            if (Boolean.TRUE.equals(deleted)) {
                logger.debug("Successfully removed orders by customer from cache: {}", customerId);
                return CacheOperationResult.success();
            } else {
                logger.debug("Orders by customer not found in cache for removal: {}", customerId);
                return CacheOperationResult.success();
            }
        } catch (Exception e) {
            logger.error("Failed to remove orders by customer from cache {}: {}", customerId, e.getMessage());
            return CacheOperationResult.failure("Failed to remove orders by customer: " + e.getMessage());
        }
    }

    // Pagination Cache (simplified implementation)
    @Override
    public CacheOperationResult cacheOrderPage(String pageKey, Page<OrderResponse> page, Duration ttl) {
        try {
            String key = getOrderPageKey(pageKey);
            String value = objectMapper.writeValueAsString(page);
            
            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                redisTemplate.opsForValue().set(key, value, ttl);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
            
            logger.debug("Successfully cached order page: {}", pageKey);
            return CacheOperationResult.success();
        } catch (Exception e) {
            logger.error("Failed to cache order page {}: {}", pageKey, e.getMessage());
            return CacheOperationResult.failure("Failed to cache order page: " + e.getMessage());
        }
    }

    @Override
    public Optional<Page<OrderResponse>> getOrderPage(String pageKey) {
        try {
            String key = getOrderPageKey(pageKey);
            String value = redisTemplate.opsForValue().get(key);
            
            if (value != null) {
                @SuppressWarnings("unchecked")
                Page<OrderResponse> page = (Page<OrderResponse>) objectMapper.readValue(value, PageImpl.class);
                logger.debug("Cache hit for order page: {}", pageKey);
                return Optional.of(page);
            } else {
                logger.debug("Cache miss for order page: {}", pageKey);
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Failed to get order page from cache {}: {}", pageKey, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public CacheOperationResult removeOrderPage(String pageKey) {
        try {
            String key = getOrderPageKey(pageKey);
            Boolean deleted = redisTemplate.delete(key);
            
            if (Boolean.TRUE.equals(deleted)) {
                logger.debug("Successfully removed order page from cache: {}", pageKey);
                return CacheOperationResult.success();
            } else {
                logger.debug("Order page not found in cache for removal: {}", pageKey);
                return CacheOperationResult.success();
            }
        } catch (Exception e) {
            logger.error("Failed to remove order page from cache {}: {}", pageKey, e.getMessage());
            return CacheOperationResult.failure("Failed to remove order page: " + e.getMessage());
        }
    }

    // Cache Invalidation
    @Override
    public CacheOperationResult invalidateAllOrderCaches(UUID orderId) {
        try {
            removeOrder(orderId);
            removeOrderResponse(orderId);
            logger.debug("Successfully invalidated all caches for order: {}", orderId);
            return CacheOperationResult.success();
        } catch (Exception e) {
            logger.error("Failed to invalidate caches for order {}: {}", orderId, e.getMessage());
            return CacheOperationResult.failure("Failed to invalidate caches: " + e.getMessage());
        }
    }

    @Override
    public CacheOperationResult invalidateCustomerOrderCaches(UUID customerId) {
        try {
            removeOrdersByCustomer(customerId);
            logger.debug("Successfully invalidated customer order caches: {}", customerId);
            return CacheOperationResult.success();
        } catch (Exception e) {
            logger.error("Failed to invalidate customer order caches {}: {}", customerId, e.getMessage());
            return CacheOperationResult.failure("Failed to invalidate customer caches: " + e.getMessage());
        }
    }

    @Override
    public CacheOperationResult invalidateStatusOrderCaches(String status) {
        try {
            removeOrdersByStatus(status);
            logger.debug("Successfully invalidated status order caches: {}", status);
            return CacheOperationResult.success();
        } catch (Exception e) {
            logger.error("Failed to invalidate status order caches {}: {}", status, e.getMessage());
            return CacheOperationResult.failure("Failed to invalidate status caches: " + e.getMessage());
        }
    }

    // Key generation methods
    private String getOrderKey(UUID orderId) {
        return "order:entity:" + orderId.toString();
    }

    private String getOrderResponseKey(UUID orderId) {
        return "order:response:" + orderId.toString();
    }

    private String getOrdersByStatusKey(String status) {
        return "orders:status:" + status.toUpperCase();
    }

    private String getOrdersByCustomerKey(UUID customerId) {
        return "orders:customer:" + customerId.toString();
    }

    private String getOrderPageKey(String pageKey) {
        return "orders:page:" + pageKey;
    }
}
