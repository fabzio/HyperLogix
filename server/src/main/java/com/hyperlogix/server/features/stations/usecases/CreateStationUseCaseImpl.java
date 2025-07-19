package com.hyperlogix.server.features.stations.usecases;

import com.hyperlogix.server.domain.Station;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CreateStationUseCaseImpl implements CreateStationUseCase {
  
  @Override
  public Station createStation(Station station) {
    // Generate ID if not provided
    if (station.getId() == null || station.getId().isEmpty()) {
      station.setId(UUID.randomUUID().toString());
    }
    
    // Set default values
    if (station.getMaxCapacity() == 0) {
      station.setMaxCapacity(160); // Default capacity
    }
    
    // Here would be the repository save operation
    // For now, just return the station with the generated values
    return station;
  }
}
