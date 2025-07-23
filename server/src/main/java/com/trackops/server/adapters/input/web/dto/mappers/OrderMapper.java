package com.trackops.server.adapters.input.web.dto.mappers;
import com.trackops.server.adapters.input.web.dto.OrderResponse;
import com.trackops.server.adapters.input.web.dto.AddressDTO;
import com.trackops.server.adapters.input.web.dto.CreateOrderRequest;
import com.trackops.server.adapters.input.web.dto.OrderResponse;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.model.orders.Address;
import java.util.List;

public class OrderMapper {

    // Order Object to OrderResponse
    public OrderResponse toOrderResponse(Order order) {

    }

    // CreateOrderRequest to Order Object
    public OrderResponse toOrder(CreateOrderRequest request) {

    }

    // AddressDTO to Address Object
    public Address toAddress(AddressDTO dto) {

    }

    // Map of a list of Order objects to a list of OrderResponse
    public List<OrderResponse> toOrderResponseList(List<Order> orders) {

    }

}