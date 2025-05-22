package com.hyperlogix.server.features.trucks.utils;

import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.features.trucks.entity.TruckEntity;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class TruckMapper {
  public static Truck mapToDomain(TruckEntity truckEntity) {
    if (truckEntity == null)
      return null;
    Truck truck = new Truck();
    truck.setId(truckEntity.getId() != null ? truckEntity.getId().toString() : null);
    truck.setCode(truckEntity.getCode());
    truck.setType(truckEntity.getType());
    truck.setStatus(truckEntity.getStatus());
    truck.setTareWeight(truckEntity.getTareWeight());
    truck.setMaxCapacity(truckEntity.getMaxCapacity());
    truck.setCurrentCapacity(truckEntity.getCurrentCapacity());
    truck.setFuelCapacity(truckEntity.getFuelCapacity());
    truck.setCurrentFuel(truckEntity.getCurrentFuel());
    truck.setNextMaintenance(truckEntity.getNextMaintenance());
    truck.setLocation(truckEntity.getLocation());
    return truck;
  }

  public static TruckEntity mapToEntity(Truck truck) {
    if (truck == null)
      return null;
    TruckEntity entity = new TruckEntity();
    if (truck.getId() != null) {
      try {
        entity.setId(Long.parseLong(truck.getId()));
      } catch (NumberFormatException e) {
        entity.setId(null);
      }
    }
    entity.setCode(truck.getCode());
    entity.setType(truck.getType());
    entity.setStatus(truck.getStatus());
    entity.setTareWeight(truck.getTareWeight());
    entity.setMaxCapacity(truck.getMaxCapacity());
    entity.setCurrentCapacity(truck.getCurrentCapacity());
    entity.setFuelCapacity(truck.getFuelCapacity());
    entity.setCurrentFuel(truck.getCurrentFuel());
    entity.setNextMaintenance(truck.getNextMaintenance());
    entity.setLocation(truck.getLocation());
    return entity;
  }
}
