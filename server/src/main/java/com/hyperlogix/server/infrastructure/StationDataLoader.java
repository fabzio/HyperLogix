package com.hyperlogix.server.infrastructure;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.hyperlogix.server.features.stations.repository.StationRepository;
import com.hyperlogix.server.features.stations.utils.StationMapper;
import com.hyperlogix.server.domain.Station;
import com.hyperlogix.server.domain.Point;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
@Profile("!prod") // Solo en desarrollo
public class StationDataLoader implements CommandLineRunner {
  @Autowired
  private StationRepository stationRepository;

  @Override
  public void run(String... args) {
    if (stationRepository.findAll().isEmpty()) {
      List<Station> stations = new ArrayList<>(
          List.of(
              new Station("S1", "Central", new Point(12, 8), Integer.MAX_VALUE, true, new HashMap<>(), new ArrayList<>()), // Adjusted
              new Station("S2", "Intermedio Norte", new Point(42, 42), 160, false, new HashMap<>(), new ArrayList<>()), // Adjusted
              new Station("S3", "Intermedio Este", new Point(63, 3), 160, false,
                  new HashMap<>(), new ArrayList<>())));
      stationRepository.saveAll(stations.stream()
          .map(StationMapper::mapToEntity).toList());
    }
  }
}
