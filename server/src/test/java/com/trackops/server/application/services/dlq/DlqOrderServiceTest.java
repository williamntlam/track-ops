package com.trackops.server.application.services.dlq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.domain.model.dlq.DlqOrder;
import com.trackops.server.ports.output.persistence.dlq.DlqOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DlqOrderService")
class DlqOrderServiceTest {

    @Mock
    private DlqOrderRepository dlqOrderRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DlqOrderService service;

    @BeforeEach
    void setUp() {
        service = new DlqOrderService(dlqOrderRepository, objectMapper);
        ReflectionTestUtils.setField(service, "defaultMaxRetries", 3);
    }

    @Test
    @DisplayName("isOrderEventTopic returns true for trackops_orders.public.orders")
    void isOrderEventTopicTrue() {
        assertThat(service.isOrderEventTopic("trackops_orders.public.orders")).isTrue();
    }

    @Test
    @DisplayName("isOrderEventTopic returns false for other topics")
    void isOrderEventTopicFalse() {
        assertThat(service.isOrderEventTopic("other.topic")).isFalse();
    }

    @Test
    @DisplayName("saveFailedOrderEvent persists with PENDING and extracts orderId from payload")
    void saveFailedOrderEvent() {
        String payload = "{\"payload\":{\"after\":{\"id\":\"22222222-2222-2222-2222-222222222222\"}}}";
        DlqOrder saved = DlqOrder.builder()
                .id(UUID.randomUUID())
                .orderId("22222222-2222-2222-2222-222222222222")
                .status("PENDING")
                .build();
        when(dlqOrderRepository.save(any(DlqOrder.class))).thenReturn(saved);

        DlqOrder result = service.saveFailedOrderEvent(
                "trackops_orders.public.orders", payload, "debezium-order-event",
                new RuntimeException("test error"));

        assertThat(result).isNotNull();
        ArgumentCaptor<DlqOrder> captor = ArgumentCaptor.forClass(DlqOrder.class);
        verify(dlqOrderRepository).save(captor.capture());
        DlqOrder captured = captor.getValue();
        assertThat(captured.getOrderId()).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(captured.getStatus()).isEqualTo("PENDING");
        assertThat(captured.getRetryCount()).isZero();
        assertThat(captured.getErrorLog()).contains("test error");
    }

    @Test
    @DisplayName("saveFailedOrderEvent uses unknown orderId when payload has no id")
    void saveFailedOrderEventNoOrderId() {
        when(dlqOrderRepository.save(any(DlqOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        service.saveFailedOrderEvent("trackops_orders.public.orders", "{}", "test", null);
        ArgumentCaptor<DlqOrder> captor = ArgumentCaptor.forClass(DlqOrder.class);
        verify(dlqOrderRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("findById delegates to repository")
    void findById() {
        UUID id = UUID.randomUUID();
        DlqOrder dlq = DlqOrder.builder().id(id).build();
        when(dlqOrderRepository.findById(id)).thenReturn(Optional.of(dlq));
        assertThat(service.findById(id)).contains(dlq);
    }
}
