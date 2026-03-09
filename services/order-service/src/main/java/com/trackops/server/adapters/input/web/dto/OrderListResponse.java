package com.trackops.server.adapters.input.web.dto;

import java.util.List;

public class OrderListResponse {
    private final List<OrderResponse> orders;
    private final int totalCount;
    private final int page;
    private final int size;
    private final boolean hasNext;
    private final boolean hasPrevious;

    public OrderListResponse(List<OrderResponse> orders, int totalCount, int page, int size, boolean hasNext, boolean hasPrevious) {
        this.orders = orders;
        this.totalCount = totalCount;
        this.page = page;
        this.size = size;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    public List<OrderResponse> getOrders() { return orders; }
    public int getTotalCount() { return totalCount; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public boolean isHasNext() { return hasNext; }
    public boolean isHasPrevious() { return hasPrevious; }

    @Override
    public String toString() {
        return "OrderListResponse{" +
                "orders=" + orders +
                ", totalCount=" + totalCount +
                ", page=" + page +
                ", size=" + size +
                ", hasNext=" + hasNext +
                ", hasPrevious=" + hasPrevious +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderListResponse that = (OrderListResponse) o;
        return totalCount == that.totalCount &&
                page == that.page &&
                size == that.size &&
                hasNext == that.hasNext &&
                hasPrevious == that.hasPrevious &&
                java.util.Objects.equals(orders, that.orders);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(orders, totalCount, page, size, hasNext, hasPrevious);
    }
} 