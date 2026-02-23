package com.trackops.server.adapters.input.messaging;

import com.trackops.server.application.services.dlq.DlqOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.kafka.listener.ListenerExecutionFailedException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DlqOrderErrorHandler")
class DlqOrderErrorHandlerTest {

    @Mock
    private DlqOrderService dlqOrderService;

    private DlqOrderErrorHandler handler;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        handler = new DlqOrderErrorHandler(dlqOrderService);
    }

    @Test
    @DisplayName("returns null when DLQ save succeeds (container will ack)")
    void handleErrorWhenDlqSaveSucceedsReturnsNull() {
        when(dlqOrderService.isOrderEventTopic("trackops_orders.public.orders")).thenReturn(true);
        when(dlqOrderService.saveFailedOrderEvent(anyString(), any(), anyString(), any())).thenReturn(null);

        Message<String> message = MessageBuilder.withPayload("{}")
                .setHeader(KafkaHeaders.RECEIVED_TOPIC, "trackops_orders.public.orders")
                .build();
        ListenerExecutionFailedException ex = new ListenerExecutionFailedException(message, "test", new RuntimeException("fail"));

        Object result = handler.handleError(message, ex);

        verify(dlqOrderService).saveFailedOrderEvent(eq("trackops_orders.public.orders"), eq("{}"), anyString(), any());
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("rethrows when DLQ save fails so offset is not committed")
    void handleErrorWhenDlqSaveFailsRethrows() {
        when(dlqOrderService.isOrderEventTopic("trackops_orders.public.orders")).thenReturn(true);
        when(dlqOrderService.saveFailedOrderEvent(anyString(), any(), anyString(), any())).thenThrow(new RuntimeException("DB error"));

        Message<String> message = MessageBuilder.withPayload("{}")
                .setHeader(KafkaHeaders.RECEIVED_TOPIC, "trackops_orders.public.orders")
                .build();
        ListenerExecutionFailedException ex = new ListenerExecutionFailedException(message, "test", new RuntimeException("original"));

        assertThatThrownBy(() -> handler.handleError(message, ex))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DLQ insert failed");
    }
}
