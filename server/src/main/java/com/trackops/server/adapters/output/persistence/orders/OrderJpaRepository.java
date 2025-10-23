package com.trackops.server.adapters.output.persistence.orders;

import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.model.enums.OrderStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;   

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderJpaRepository extends JpaRepository<Order, UUID> {

    List<Order> findByStatus(OrderStatus status);
    List<Order> findByCustomerId(UUID customerId);

}