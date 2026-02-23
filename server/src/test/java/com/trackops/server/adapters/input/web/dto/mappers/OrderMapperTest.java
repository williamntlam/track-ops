package com.trackops.server.adapters.input.web.dto.mappers;

import com.trackops.server.adapters.input.web.dto.AddressDTO;
import com.trackops.server.adapters.input.web.dto.CreateOrderRequest;
import com.trackops.server.adapters.input.web.dto.OrderResponse;
import com.trackops.server.domain.model.orders.Address;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.model.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderMapper")
class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    @Test
    @DisplayName("orderToOrderResponse returns null when order is null")
    void orderToOrderResponseNull() {
        assertThat(mapper.orderToOrderResponse(null)).isNull();
    }

    @Test
    @DisplayName("orderToOrderResponse maps order fields to response")
    void orderToOrderResponseMapsFields() {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.TEN);
        order.setAddress(null);
        order.setDeliveryInstructions("notes");
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        OrderResponse response = mapper.orderToOrderResponse(order);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getCustomerId()).isEqualTo(customerId);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(response.getDeliveryInstructions()).isEqualTo("notes");
    }

    @Test
    @DisplayName("createOrderRequestToOrder returns null when request is null")
    void createOrderRequestToOrderNull() {
        assertThat(mapper.createOrderRequestToOrder(null)).isNull();
    }

    @Test
    @DisplayName("createOrderRequestToOrder maps request to order with PENDING status")
    void createOrderRequestToOrderMapsFields() {
        AddressDTO addressDto = new AddressDTO("123 St", "City", "ST", "12345", "Country", null);
        CreateOrderRequest request = new CreateOrderRequest(
                UUID.randomUUID(), new BigDecimal("99.99"), addressDto, "instructions");

        Order order = mapper.createOrderRequestToOrder(request);

        assertThat(order).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(request.getCustomerId());
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getAddress()).isNotNull();
        assertThat(order.getAddress().getStreetAddress()).isEqualTo("123 St");
    }

    @Test
    @DisplayName("addressToAddressDTO returns null when address is null")
    void addressToAddressDTONull() {
        assertThat(mapper.addressToAddressDTO(null)).isNull();
    }

    @Test
    @DisplayName("addressDTOToAddress returns null when dto is null")
    void addressDTOToAddressNull() {
        assertThat(mapper.addressDTOToAddress(null)).isNull();
    }
}
