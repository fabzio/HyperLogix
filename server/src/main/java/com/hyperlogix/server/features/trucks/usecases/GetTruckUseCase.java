package com.hyperlogix.server.features.trucks.usecases;

import org.springframework.stereotype.Service;

import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.features.trucks.repository.TruckRepository;
import com.hyperlogix.server.features.trucks.utils.TruckMapper;

@Service
public class GetTruckUseCase {
  private final TruckRepository truckRepository;

  public GetTruckUseCase(TruckRepository truckRepository) {
    this.truckRepository = truckRepository;
  }

  public Truck getTruckById(String truckId) {
    return TruckMapper.mapToDomain(truckRepository.findById(Long.parseLong(truckId))
        .orElseThrow(() -> new IllegalArgumentException("Truck not found with ID: " + truckId)));
  }
}
