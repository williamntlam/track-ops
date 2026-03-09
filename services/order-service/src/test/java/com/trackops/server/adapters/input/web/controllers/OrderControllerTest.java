package com.trackops.server.adapters.input.web.controllers;

import com.trackops.server.adapters.input.web.dto.OrderResponse;
import com.trackops.server.adapters.input.web.dto.mappers.OrderMapper;
import com.trackops.server.domain.model.enums.OrderStatus;
import com.trackops.server.ports.input.orders.OrderServicePort;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrderControllerTest {

    private final OrderServicePort orderService = mock(OrderServicePort.class);
    private final OrderMapper orderMapper = mock(OrderMapper.class);
    private final OrderController controller = new OrderController(orderService, orderMapper);

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Test
    void getOrderByIdReturnsOkWhenFound() {
        OrderResponse response = new OrderResponse(
                ORDER_ID, CUSTOMER_ID, OrderStatus.PENDING, BigDecimal.TEN,
                null, null, Instant.now(), Instant.now());
        when(orderService.getOrderById(ORDER_ID)).thenReturn(response);
        ResponseEntity<OrderResponse> result = controller.getOrderById(ORDER_ID);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(ORDER_ID);
        verify(orderService).getOrderById(ORDER_ID);
    }

    @Test
    void getOrderByIdReturnsNotFoundWhenNull() {
        when(orderService.getOrderById(ORDER_ID)).thenReturn(null);
        ResponseEntity<OrderResponse> result = controller.getOrderById(ORDER_ID);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isNull();
    }

    @Test
    void cancelOrderReturnsNoContent() {
        when(orderService.cancelOrder(ORDER_ID)).thenReturn(null);
        ResponseEntity<Void> result = controller.cancelOrder(ORDER_ID);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(orderService).cancelOrder(ORDER_ID);
    }
}
