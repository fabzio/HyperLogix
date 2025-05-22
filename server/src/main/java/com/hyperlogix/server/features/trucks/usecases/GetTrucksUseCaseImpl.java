package com.hyperlogix.server.features.trucks.usecases;

import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.features.trucks.repository.TruckRepository;
import com.hyperlogix.server.features.trucks.utils.TruckMapper;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Service
public class GetTrucksUseCaseImpl implements GetTrucksUseCase {
  private final TruckRepository truckRepository;

  public GetTrucksUseCaseImpl(TruckRepository truckRepository) {
    this.truckRepository = truckRepository;
  }

  @Override
  public List<Truck> getAllTrucks() {
    return truckRepository.findAll().stream()
        .map(TruckMapper::mapToDomain)
        .toList();
  }

  @Override
  public Page<Truck> getAllTrucks(Pageable pageable) {
    return truckRepository.findAll(pageable).map(TruckMapper::mapToDomain);
  }
}
