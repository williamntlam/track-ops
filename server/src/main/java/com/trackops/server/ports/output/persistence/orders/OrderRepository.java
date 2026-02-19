package com.trackops.server.ports.output.persistence.orders;

import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);
    Optional<Order> findById(UUID id);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByCustomerId(UUID customerId);
    List<Order> findAll();
    Page<Order> findAll(Pageable pageable);
    /**
     * Find most recently updated orders first (for cache pre-warming).
     */
    Page<Order> findMostRecentlyUpdated(Pageable pageable);
    void deleteById(UUID id);
    boolean existsById(UUID id);

}