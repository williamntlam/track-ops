package com.trackops.server.adapters.output.messaging;

import com.trackops.server.adapters.input.web.dto.OrderResponse;
import com.trackops.server.application.services.events.MaterializedViewProjectionService;
import com.trackops.server.domain.model.eventstore.OrderEvent;
import com.trackops.server.ports.output.cache.OrderMaterializedViewPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Listens to order events appended to the event store and updates the Redis materialized view
 * (update-on-write). Uses {@link MaterializedViewProjectionService} so projection logic stays
 * in sync with read-through rebuild. For up-to-date reads use {@link MaterializedViewProjectionService#getViewUpToDate(UUID)}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaterializedViewUpdateListener {

    private final OrderMaterializedViewPort materializedViewPort;
    private final MaterializedViewProjectionService projectionService;

    @EventListener
    public void onOrderEventAppended(OrderEvent event) {
        UUID orderId = event.getOrderId();
        String eventType = event.getEventType();
        try {
            Optional<OrderResponse> currentOpt = materializedViewPort.getView(orderId);
            Optional<OrderResponse> viewToWrite = projectionService.applyEvent(event, currentOpt, orderId);
            if (viewToWrite.isEmpty()) return;
            materializedViewPort.putView(orderId, viewToWrite.get(), projectionService.getViewTtl());
            log.debug("Updated materialized view for order {} after {}", orderId, eventType);
        } catch (Exception e) {
            log.error("Failed to update materialized view for order {} on {}: {}", orderId, eventType, e.getMessage(), e);
        }
    }
}
