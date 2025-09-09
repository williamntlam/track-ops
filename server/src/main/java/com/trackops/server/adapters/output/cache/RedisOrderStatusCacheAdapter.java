package com.trackops.server.adapters.output.cache;

import com.trackops.server.ports.output.cache.OrderStatusCachePort;
import com.trackops.server.domain.model.enums.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class RedisOrderStatusCacheAdapter implements OrderStatusCachePort {
    
}