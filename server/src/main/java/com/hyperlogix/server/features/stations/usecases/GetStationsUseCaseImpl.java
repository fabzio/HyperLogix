package com.hyperlogix.server.features.stations.usecases;

import com.hyperlogix.server.domain.Station;
import com.hyperlogix.server.features.stations.repository.StationRepository;
import com.hyperlogix.server.features.stations.utils.StationMapper;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Service
public class GetStationsUseCaseImpl implements GetStationsUseCase {
  private final StationRepository stationRepository;

  public GetStationsUseCaseImpl(StationRepository stationRepository) {
    this.stationRepository = stationRepository;
  }

  @Override
  public List<Station> getAllStations() {
    return stationRepository.findAll().stream()
        .map(StationMapper::mapToDomain)
        .toList();
  }

  @Override
  public Page<Station> getAllStations(Pageable pageable) {
    return stationRepository.findAll(pageable).map(StationMapper::mapToDomain);
  }
}
