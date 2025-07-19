package com.hyperlogix.server.features.stations.usecases;

import com.hyperlogix.server.domain.Station;

public interface UpdateStationUseCase {
  Station updateStation(String id, Station station);
}
