package com.trackops.server.adapters.output.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.adapters.input.web.dto.OrderResponse;
import com.trackops.server.ports.output.cache.OrderMaterializedViewPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed materialized view for order read model.
 * Key: order:view:{order_id}, Value: JSON (OrderResponse). TTL configurable (e.g. 24h).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisOrderMaterializedViewAdapter implements OrderMaterializedViewPort {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<OrderResponse> getView(UUID orderId) {
        try {
            String key = viewKey(orderId);
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            OrderResponse view = objectMapper.readValue(value, OrderResponse.class);
            return Optional.of(view);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize materialized view for order {}: {}", orderId, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get materialized view for order {}", orderId, e);
            return Optional.empty();
        }
    }

    @Override
    public void putView(UUID orderId, OrderResponse view, Duration ttl) {
        try {
            String key = viewKey(orderId);
            String value = objectMapper.writeValueAsString(view);
            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                redisTemplate.opsForValue().set(key, value, ttl);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
            log.debug("Updated materialized view for order {}", orderId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize materialized view for order {}", orderId, e);
            throw new RuntimeException("Failed to write materialized view", e);
        } catch (Exception e) {
            log.error("Failed to put materialized view for order {}", orderId, e);
            throw new RuntimeException("Failed to write materialized view", e);
        }
    }

    @Override
    public void removeView(UUID orderId) {
        try {
            redisTemplate.delete(viewKey(orderId));
            log.debug("Removed materialized view for order {}", orderId);
        } catch (Exception e) {
            log.warn("Failed to remove materialized view for order {}: {}", orderId, e.getMessage());
        }
    }

    private static String viewKey(UUID orderId) {
        return OrderMaterializedViewPort.VIEW_KEY_PREFIX + orderId.toString();
    }
}
