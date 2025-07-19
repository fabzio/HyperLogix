package com.hyperlogix.server.features.trucks.usecases;

import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.features.trucks.entity.TruckEntity;
import com.hyperlogix.server.features.trucks.repository.TruckRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateTruckUseCaseImpl implements UpdateTruckUseCase {
  
  private final TruckRepository truckRepository;
  
  public UpdateTruckUseCaseImpl(TruckRepository truckRepository) {
    this.truckRepository = truckRepository;
  }
  
  @Override
  public Truck updateTruck(String id, Truck truck) {
    // Find existing truck
    TruckEntity existingTruck = truckRepository.findById(Long.valueOf(id))
        .orElseThrow(() -> new RuntimeException("Truck not found with id: " + id));
    
    // Update fields
    existingTruck.setCode(truck.getCode());
    existingTruck.setType(truck.getType());
    existingTruck.setStatus(truck.getStatus());
    existingTruck.setTareWeight(truck.getTareWeight());
    existingTruck.setMaxCapacity(truck.getMaxCapacity());
    existingTruck.setCurrentCapacity(truck.getCurrentCapacity());
    existingTruck.setFuelCapacity(truck.getFuelCapacity());
    existingTruck.setCurrentFuel(truck.getCurrentFuel());
    existingTruck.setLocation(truck.getLocation());
    
    // Validate fuel capacity
    if (existingTruck.getCurrentFuel() > existingTruck.getFuelCapacity()) {
      existingTruck.setCurrentFuel(existingTruck.getFuelCapacity());
    }
    
    // Validate capacity
    if (existingTruck.getCurrentCapacity() > existingTruck.getMaxCapacity()) {
      existingTruck.setCurrentCapacity(existingTruck.getMaxCapacity());
    }
    
    // Save to database
    TruckEntity savedTruck = truckRepository.save(existingTruck);
    
    // Convert back to domain object
    return convertToTruck(savedTruck);
  }
  
  private Truck convertToTruck(TruckEntity entity) {
    Truck truck = new Truck();
    truck.setId(entity.getId().toString());
    truck.setCode(entity.getCode());
    truck.setType(entity.getType());
    truck.setStatus(entity.getStatus());
    truck.setTareWeight(entity.getTareWeight());
    truck.setMaxCapacity(entity.getMaxCapacity());
    truck.setCurrentCapacity(entity.getCurrentCapacity());
    truck.setFuelCapacity(entity.getFuelCapacity());
    truck.setCurrentFuel(entity.getCurrentFuel());
    truck.setNextMaintenance(entity.getNextMaintenance());
    truck.setLocation(entity.getLocation());
    return truck;
  }
}
