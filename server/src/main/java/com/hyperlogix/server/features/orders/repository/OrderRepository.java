package com.hyperlogix.server.features.orders.repository;

import com.hyperlogix.server.features.orders.entity.OrderEntity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
  List<OrderEntity> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}
