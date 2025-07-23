package com.trackops.server.adapters.input.web.dto;

import java.util.UUID;
import java.util.BigDecimal;
import com.trackops.server.adapters.input.web.dto.AddressDTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

public class CreateOrderRequest {

    @NotNull
    private UUID customerId;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal totalAmount;
    @NotNull
    @Valid
    private AddressDTO address;
    @Size(max = 500)
    private String deliveryInstructions;

    public CreateOrderRequest() {}

    public CreateOrderRequest(UUID customerId, BigDecimal totalAmount, AddressDTO address, String deliveryInstructions) {
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.address = address;
        this.deliveryInstructions = deliveryInstructions;
    }

}