package com.trackops.server.adapters.output.messaging.inventory;

import com.trackops.server.ports.output.persistence.outbox.InventoryReserveOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxInventoryReservationRequestAdapter")
class OutboxInventoryReservationRequestAdapterTest {

    @Mock
    private InventoryReserveOutboxRepository outboxRepository;

    private OutboxInventoryReservationRequestAdapter adapter;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        adapter = new OutboxInventoryReservationRequestAdapter(outboxRepository);
    }

    @Test
    @DisplayName("requestReservation calls enqueueIfAbsent with orderId")
    void requestReservationEnqueues() {
        UUID orderId = UUID.randomUUID();
        when(outboxRepository.enqueueIfAbsent(orderId)).thenReturn(true);

        adapter.requestReservation(orderId);

        verify(outboxRepository, times(1)).enqueueIfAbsent(orderId);
    }

    @Test
    @DisplayName("requestReservation is idempotent when already enqueued")
    void requestReservationIdempotent() {
        UUID orderId = UUID.randomUUID();
        when(outboxRepository.enqueueIfAbsent(orderId)).thenReturn(false);

        adapter.requestReservation(orderId);

        verify(outboxRepository, times(1)).enqueueIfAbsent(orderId);
    }
}
