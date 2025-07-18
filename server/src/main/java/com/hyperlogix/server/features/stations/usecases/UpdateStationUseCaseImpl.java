package com.hyperlogix.server.features.stations.usecases;

import com.hyperlogix.server.domain.Station;
import com.hyperlogix.server.features.stations.entity.StationEntity;
import com.hyperlogix.server.features.stations.repository.StationRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateStationUseCaseImpl implements UpdateStationUseCase {
  
  private final StationRepository stationRepository;
  
  public UpdateStationUseCaseImpl(StationRepository stationRepository) {
    this.stationRepository = stationRepository;
  }
  
  @Override
  public Station updateStation(String id, Station station) {
    // Find existing station
    StationEntity existingStation = stationRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Station not found with id: " + id));
    
    // Update fields
    existingStation.setName(station.getName());
    existingStation.setMaxCapacity(station.getMaxCapacity());
    existingStation.setLocation(station.getLocation());
    existingStation.setMainStation(station.isMainStation());
    
    // Validate capacity (use Integer.MAX_VALUE for infinite capacity)
    if (existingStation.getMaxCapacity() <= 0 && !existingStation.isMainStation()) {
      existingStation.setMaxCapacity(160); // Default capacity
    }
    
    // Save to database
    StationEntity savedStation = stationRepository.save(existingStation);
    
    // Convert back to domain object
    return convertToStation(savedStation);
  }
  
  private Station convertToStation(StationEntity entity) {
    Station station = new Station();
    station.setId(entity.getId());
    station.setName(entity.getName());
    station.setMaxCapacity(entity.getMaxCapacity());
    station.setLocation(entity.getLocation());
    station.setMainStation(entity.isMainStation());
    return station;
  }
}
