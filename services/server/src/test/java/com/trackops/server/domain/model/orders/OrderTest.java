package com.trackops.server.domain.model.orders;

import com.trackops.server.domain.model.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Test
    void confirmFromPendingTransitionsToConfirmed() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        order.confirm();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void confirmFromNonPendingThrows() {
        Order order = orderWithStatus(OrderStatus.CONFIRMED);
        assertThatThrownBy(order::confirm)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending orders can be confirmed");
    }

    @Test
    void processFromConfirmedTransitionsToProcessing() {
        Order order = orderWithStatus(OrderStatus.CONFIRMED);
        order.process();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void cancelFromPendingTransitionsToCancelled() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        order.cancel();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelFromDeliveredThrows() {
        Order order = orderWithStatus(OrderStatus.DELIVERED);
        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Delivered orders cannot be cancelled");
    }

    @Test
    void addOrderItemNullThrows() {
        Order order = new Order(CUSTOMER_ID, OrderStatus.PENDING, BigDecimal.TEN, null, null);
        order.setId(UUID.randomUUID());
        assertThatThrownBy(() -> order.addOrderItem(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order item cannot be null");
    }

    private static Order orderWithStatus(OrderStatus status) {
        Order order = new Order(CUSTOMER_ID, status, BigDecimal.TEN, null, null);
        order.setId(UUID.randomUUID());
        return order;
    }
}
