package com.hyperlogix.server.features.trucks.usecases;

import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.domain.TruckType;
import com.hyperlogix.server.domain.TruckState;
import com.hyperlogix.server.domain.Point;
import com.hyperlogix.server.features.trucks.entity.TruckEntity;
import com.hyperlogix.server.features.trucks.repository.TruckRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CreateTruckUseCaseImpl implements CreateTruckUseCase {
  
  private final TruckRepository truckRepository;
  
  public CreateTruckUseCaseImpl(TruckRepository truckRepository) {
    this.truckRepository = truckRepository;
  }
  
  @Override
  public Truck createTruck(Truck truck) {
    // Create entity
    TruckEntity entity = new TruckEntity();
    
    // Set fields from truck
    entity.setCode(truck.getCode());
    entity.setType(truck.getType());
    entity.setStatus(truck.getStatus() != null ? truck.getStatus() : TruckState.ACTIVE);
    entity.setTareWeight(truck.getTareWeight());
    entity.setMaxCapacity(truck.getMaxCapacity());
    entity.setCurrentCapacity(truck.getCurrentCapacity());
    entity.setFuelCapacity(truck.getFuelCapacity());
    entity.setCurrentFuel(truck.getCurrentFuel() > 0 ? truck.getCurrentFuel() : truck.getFuelCapacity());
    entity.setLocation(truck.getLocation() != null ? truck.getLocation() : new Point(0, 0));
    
    // Set default values
    if (entity.getNextMaintenance() == null) {
      entity.setNextMaintenance(LocalDateTime.now().plusYears(1));
    }
    
    // Generate code if not provided
    if (entity.getCode() == null || entity.getCode().isEmpty()) {
      entity.setCode(generateTruckCode(entity.getType()));
    }
    
    // Validate fuel capacity
    if (entity.getCurrentFuel() > entity.getFuelCapacity()) {
      entity.setCurrentFuel(entity.getFuelCapacity());
    }
    
    // Validate capacity
    if (entity.getCurrentCapacity() > entity.getMaxCapacity()) {
      entity.setCurrentCapacity(entity.getMaxCapacity());
    }
    
    // Save to database
    TruckEntity savedTruck = truckRepository.save(entity);
    
    // Convert back to domain object
    return convertToTruck(savedTruck);
  }
  
  private String generateTruckCode(TruckType type) {
    // Simple code generation: Type + timestamp
    return type.name() + "-" + System.currentTimeMillis() % 10000;
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
