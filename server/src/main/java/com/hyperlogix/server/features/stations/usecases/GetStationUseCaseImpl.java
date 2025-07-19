package com.hyperlogix.server.features.stations.usecases;

import com.hyperlogix.server.domain.Station;
import org.springframework.stereotype.Service;

@Service
public class GetStationUseCaseImpl implements GetStationUseCase {
  
  @Override
  public Station getStationById(String id) {
    // Here would be the repository find operation
    // For now, return a mock station
    Station station = new Station();
    station.setId(id);
    station.setName("Station " + id);
    station.setMaxCapacity(160);
    return station;
  }
}
