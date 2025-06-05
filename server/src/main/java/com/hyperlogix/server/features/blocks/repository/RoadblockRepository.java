package com.hyperlogix.server.features.blocks.repository;

import com.hyperlogix.server.features.blocks.entity.BlockEntity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadblockRepository extends JpaRepository<BlockEntity, Long> {
  List<BlockEntity> findByStartTimeBetween(LocalDateTime starTime, LocalDateTime endTime);
}