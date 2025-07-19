package com.hyperlogix.server.features.trucks.usecases;

import com.hyperlogix.server.domain.Truck;

public interface CreateTruckUseCase {
  Truck createTruck(Truck truck);
}
