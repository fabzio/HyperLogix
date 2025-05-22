package com.hyperlogix.server.features.trucks.usecases;

import com.hyperlogix.server.domain.Truck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GetTrucksUseCase {
  List<Truck> getAllTrucks();

  Page<Truck> getAllTrucks(Pageable pageable);
}
