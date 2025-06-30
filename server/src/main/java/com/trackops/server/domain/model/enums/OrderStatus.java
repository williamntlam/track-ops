package com.trackops.server.domain.model.orders;

public enum OrderStatus {
    PENDING,
    CONFIRMED, 
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}