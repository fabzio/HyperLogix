package com.hyperlogix.server.features.orders.repository;

import com.hyperlogix.server.features.orders.entity.OrderEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
}
