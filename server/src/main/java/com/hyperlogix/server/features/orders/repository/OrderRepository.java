package com.hyperlogix.server.features.orders.repository;

import com.hyperlogix.server.features.orders.entity.OrderEntity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {
  List<OrderEntity> findByDateBetweenOrderByDateAsc(LocalDateTime startDate, LocalDateTime endDate);
}
