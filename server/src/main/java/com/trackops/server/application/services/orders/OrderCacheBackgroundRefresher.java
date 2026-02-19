package com.trackops.server.application.services.orders;

import com.trackops.server.adapters.input.web.dto.mappers.OrderMapper;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.ports.output.cache.OrderCachePort;
import com.trackops.server.ports.output.persistence.orders.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Triggers best-effort background refresh of order cache (probabilistic early revalidation).
 * Does not hold the load lock; used when we decide to refresh on a cache hit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCacheBackgroundRefresher {

    private final OrderRepository orderRepository;
    private final OrderCachePort orderCachePort;
    private final OrderMapper orderMapper;

    @Async("cachePrewarmExecutor")
    public void refreshOrderAsync(UUID orderId) {
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                log.debug("Probabilistic refresh skipped: order not found {}", orderId);
                return;
            }
            Order order = orderOpt.get();
            var response = orderMapper.orderToOrderResponse(order);
            if (response == null) return;
            orderCachePort.cacheOrder(order, Duration.ofHours(1));
            orderCachePort.cacheOrderResponse(orderId, response, Duration.ofHours(1));
            log.debug("Probabilistic refresh completed for order {}", orderId);
        } catch (Exception e) {
            log.warn("Probabilistic refresh failed for order {}: {}", orderId, e.getMessage());
        }
    }
}
