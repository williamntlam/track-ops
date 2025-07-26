package com.trackops.server.adapters.input.web.dto.mappers;
import com.trackops.server.adapters.input.web.dto.OrderResponse;
import com.trackops.server.adapters.input.web.dto.AddressDTO;
import com.trackops.server.adapters.input.web.dto.CreateOrderRequest;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.model.orders.Address;
import java.util.List;
import java.util.stream.Collectors;

public class OrderMapper {

    // Order Object to OrderResponse
    public OrderResponse orderToOrderResponse(Order order) {
        if (order == null) {
            return null;
        }
        
        return new OrderResponse(
            order.getId(),
            order.getCustomerId(),
            order.getStatus(),
            order.getTotalAmount(),
            addressToAddressDTO(order.getAddress()),
            order.getDeliveryInstructions(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    // CreateOrderRequest to Order Object
    public Order createOrderRequestToOrder(CreateOrderRequest request) {
        if (request == null) {
            return null;
        }
        
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setTotalAmount(request.getTotalAmount());
        order.setAddress(addressDTOToAddress(request.getAddress()));
        order.setDeliveryInstructions(request.getDeliveryInstructions());
        order.setStatus(com.trackops.server.domain.model.enums.OrderStatus.PENDING);
        order.setCreatedAt(java.time.Instant.now());
        order.setUpdatedAt(java.time.Instant.now());
        
        return order;
    }

    // AddressDTO to Address Object
    public Address addressDTOToAddress(AddressDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return new Address(
            dto.getStreetAddress(),
            dto.getCity(),
            dto.getState(),
            dto.getPostalCode(),
            dto.getCountry(),
            dto.getPhoneNumber()
        );
    }

    // Address Object to AddressDTO
    public AddressDTO addressToAddressDTO(Address address) {
        if (address == null) {
            return null;
        }
        
        return new AddressDTO(
            address.getStreetAddress(),
            address.getCity(),
            address.getState(),
            address.getPostalCode(),
            address.getCountry(),
            address.getPhoneNumber()
        );
    }

    // Map of a list of Order objects to a list of OrderResponse
    public List<OrderResponse> createOrderResponseList(List<Order> orders) {
        if (orders == null) {
            return java.util.Collections.emptyList();
        }
        
        return orders.stream()
            .map(this::orderToOrderResponse)
            .collect(Collectors.toList());
    }

}