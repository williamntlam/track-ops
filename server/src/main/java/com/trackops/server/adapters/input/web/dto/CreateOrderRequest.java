package com.trackops.server.adapters.input.web.dto;

import java.util.UUID;
import java.math.BigDecimal;
import com.trackops.server.adapters.input.web.dto.AddressDTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.Valid;
import com.trackops.server.adapters.input.web.validation.ValidCustomerId;

public class CreateOrderRequest {

    @NotNull(message = "Customer ID is required")
    @ValidCustomerId
    private UUID customerId;
    
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be at least $0.01")
    @DecimalMax(value = "999999.99", message = "Total amount cannot exceed $999,999.99")
    private BigDecimal totalAmount;
    
    @NotNull(message = "Address is required")
    @Valid
    private AddressDTO address;
    
    @Size(max = 500, message = "Delivery instructions must not exceed 500 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\s\\-.,!?()]{0,500}$", message = "Delivery instructions can only contain letters, numbers, spaces, and basic punctuation")
    private String deliveryInstructions;

    public CreateOrderRequest() {}

    public CreateOrderRequest(UUID customerId, BigDecimal totalAmount, AddressDTO address, String deliveryInstructions) {
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.address = address;
        this.deliveryInstructions = deliveryInstructions;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public AddressDTO getAddress() {
        return address;
    }

    public void setAddress(AddressDTO address) {
        this.address = address;
    }

    public String getDeliveryInstructions() {
        return deliveryInstructions;
    }

    public void setDeliveryInstructions(String deliveryInstructions) {
        this.deliveryInstructions = deliveryInstructions;
    }

    @Override
    public String toString() {
        return "CreateOrderRequest{" +
                "customerId=" + customerId +
                ", totalAmount=" + totalAmount +
                ", address=" + address +
                ", deliveryInstructions='" + deliveryInstructions + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateOrderRequest that = (CreateOrderRequest) o;
        return java.util.Objects.equals(customerId, that.customerId) &&
                java.util.Objects.equals(totalAmount, that.totalAmount) &&
                java.util.Objects.equals(address, that.address) &&
                java.util.Objects.equals(deliveryInstructions, that.deliveryInstructions);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(customerId, totalAmount, address, deliveryInstructions);
    }
}