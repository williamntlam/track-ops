package com.trackops.server.adapters.input.web.dto;

import com.trackops.server.domain.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class OrderResponse {
    private final UUID id;
    private final UUID customerId;
    private final OrderStatus status;
    private final BigDecimal totalAmount;
    private final AddressDTO address;
    private final String deliveryInstructions;
    private final Instant createdAt;
    private final Instant updatedAt;

    public OrderResponse(UUID id, UUID customerId, OrderStatus status, BigDecimal totalAmount, AddressDTO address, String deliveryInstructions, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.address = address;
        this.deliveryInstructions = deliveryInstructions;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public AddressDTO getAddress() { return address; }
    public String getDeliveryInstructions() { return deliveryInstructions; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public String toString() {
        return "OrderResponse{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                ", address=" + address +
                ", deliveryInstructions='" + deliveryInstructions + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderResponse that = (OrderResponse) o;
        return java.util.Objects.equals(id, that.id) &&
                java.util.Objects.equals(customerId, that.customerId) &&
                status == that.status &&
                java.util.Objects.equals(totalAmount, that.totalAmount) &&
                java.util.Objects.equals(address, that.address) &&
                java.util.Objects.equals(deliveryInstructions, that.deliveryInstructions) &&
                java.util.Objects.equals(createdAt, that.createdAt) &&
                java.util.Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, customerId, status, totalAmount, address, deliveryInstructions, createdAt, updatedAt);
    }

    // Optionally, add a static factory method fromEntity(Order order) here if you want
} 