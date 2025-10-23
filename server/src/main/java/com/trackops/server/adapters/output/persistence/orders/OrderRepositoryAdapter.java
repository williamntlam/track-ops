package com.trackops.server.adapters.output.persistence.orders;
import com.trackops.server.domain.model.enums.OrderStatus;

import com.trackops.server.ports.output.persistence.orders.OrderRepository;
import com.trackops.server.domain.model.orders.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public class OrderRepositoryAdapter implements OrderRepository {
    
    private final OrderJpaRepository orderJpaRepository;

    public OrderRepositoryAdapter(OrderJpaRepository orderJpaRepository) {

        this.orderJpaRepository = orderJpaRepository;

    }

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return orderJpaRepository.findById(id);
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return orderJpaRepository.findByStatus(status);
    }

    @Override
    public List<Order> findByCustomerId(UUID customerId) {
        return orderJpaRepository.findByCustomerId(customerId);
    }

    @Override
    public List<Order> findAll() {
        return orderJpaRepository.findAll();
    }

    @Override
    public Page<Order> findAll(Pageable pageable) {
        return orderJpaRepository.findAll(pageable);
    }

    @Override
    public void deleteById(UUID id) {
        orderJpaRepository.deleteById(id);
    }

     @Override
    public boolean existsById(UUID id) {
        return orderJpaRepository.existsById(id);
    }

}