package com.hyperlogix.server.features.trucks.usecases;

import com.hyperlogix.server.domain.Truck;

public interface UpdateTruckUseCase {
  Truck updateTruck(String id, Truck truck);
}
