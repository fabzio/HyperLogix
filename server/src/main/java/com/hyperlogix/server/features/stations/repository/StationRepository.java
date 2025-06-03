package com.hyperlogix.server.features.stations.repository;

import com.hyperlogix.server.features.stations.entity.StationEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<StationEntity, String> {
}
