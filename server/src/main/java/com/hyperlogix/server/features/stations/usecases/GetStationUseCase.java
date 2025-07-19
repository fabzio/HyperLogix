package com.hyperlogix.server.features.stations.usecases;

import com.hyperlogix.server.domain.Station;

public interface GetStationUseCase {
  Station getStationById(String id);
}
