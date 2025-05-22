package com.hyperlogix.server.features.stations.usecases;

import com.hyperlogix.server.domain.Station;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetStationsUseCase {
  List<Station> getAllStations();

  Page<Station> getAllStations(Pageable pageable);
}
