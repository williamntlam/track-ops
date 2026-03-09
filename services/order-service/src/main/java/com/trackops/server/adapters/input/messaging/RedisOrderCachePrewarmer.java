package com.trackops.server.adapters.input.messaging;

import com.trackops.server.adapters.input.web.dto.mappers.OrderMapper;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.ports.output.cache.OrderCachePort;
import com.trackops.server.ports.output.persistence.orders.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Proactively warms the Redis order cache on application startup with the top N
 * most recently updated (active) orders so that first-touch latency is reduced.
 * Runs asynchronously so it does not block the main thread.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisOrderCachePrewarmer {

    private final OrderRepository orderRepository;
    private final OrderCachePort orderCachePort;
    private final OrderMapper orderMapper;

    @org.springframework.beans.factory.annotation.Value("${app.cache.prewarm.size:1000}")
    private int prewarmSize;

    @org.springframework.beans.factory.annotation.Value("${app.cache.ttl.order:3600}")
    private long orderCacheTtlSeconds;

    @EventListener(ApplicationReadyEvent.class)
    @Async("cachePrewarmExecutor")
    public void onApplicationReady() {
        log.info("Starting Redis order cache pre-warm (top {} most recently updated orders)", prewarmSize);
        try {
            var page = orderRepository.findMostRecentlyUpdated(PageRequest.of(0, prewarmSize));
            List<Order> orders = page.getContent();
            Duration ttl = Duration.ofSeconds(orderCacheTtlSeconds);
            int cached = 0;
            for (Order order : orders) {
                try {
                    orderCachePort.cacheOrder(order, ttl);
                    var response = orderMapper.orderToOrderResponse(order);
                    if (response != null) {
                        orderCachePort.cacheOrderResponse(order.getId(), response, ttl);
                    }
                    cached++;
                } catch (Exception e) {
                    log.warn("Failed to cache order {} during pre-warm: {}", order.getId(), e.getMessage());
                }
            }
            log.info("Redis order cache pre-warm completed: {} orders cached", cached);
        } catch (Exception e) {
            log.error("Redis order cache pre-warm failed", e);
        }
    }
}
