package com.trackops.server.adapters.input.web.dto;

import com.trackops.server.domain.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateOrderStatusRequest {

    @NotNull
    private OrderStatus newStatus;

    public UpdateOrderStatusRequest() {}

    public UpdateOrderStatusRequest(OrderStatus newStatus) {
        this.newStatus = newStatus;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(OrderStatus newStatus) {
        this.newStatus = newStatus;
    }

    @Override
    public String toString() {
        return "UpdateOrderStatusRequest{" +
                "newStatus=" + newStatus +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateOrderStatusRequest that = (UpdateOrderStatusRequest) o;
        return newStatus == that.newStatus;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(newStatus);
    }
}