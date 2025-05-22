package com.hyperlogix.server.features.trucks.repository;

import com.hyperlogix.server.features.trucks.entity.TruckEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TruckRepository extends JpaRepository<TruckEntity, Long> {
}
