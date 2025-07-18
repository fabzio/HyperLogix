package com.hyperlogix.server.features.stations.controllers;

import com.hyperlogix.server.features.stations.usecases.CreateStationUseCase;
import com.hyperlogix.server.features.stations.usecases.DeleteStationUseCase;
import com.hyperlogix.server.features.stations.usecases.GetStationUseCase;
import com.hyperlogix.server.features.stations.usecases.GetStationsUseCase;
import com.hyperlogix.server.features.stations.usecases.UpdateStationUseCase;
import com.hyperlogix.server.domain.Station;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stations")
public class StationController {
  private final GetStationsUseCase getStationsUseCase;
  private final GetStationUseCase getStationUseCase;
  private final CreateStationUseCase createStationUseCase;
  private final UpdateStationUseCase updateStationUseCase;
  private final DeleteStationUseCase deleteStationUseCase;

  public StationController(
      GetStationsUseCase getStationsUseCase,
      GetStationUseCase getStationUseCase,
      CreateStationUseCase createStationUseCase,
      UpdateStationUseCase updateStationUseCase,
      DeleteStationUseCase deleteStationUseCase) {
    this.getStationsUseCase = getStationsUseCase;
    this.getStationUseCase = getStationUseCase;
    this.createStationUseCase = createStationUseCase;
    this.updateStationUseCase = updateStationUseCase;
    this.deleteStationUseCase = deleteStationUseCase;
  }

  @GetMapping
  public Page<Station> list(@PageableDefault(size = 20) Pageable pageable) {
    return getStationsUseCase.getAllStations(pageable);
  }

  @GetMapping("/{stationId}")
  public Station getStationById(@PathVariable String stationId) {
    return getStationUseCase.getStationById(stationId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Station createStation(@RequestBody Station station) {
    return createStationUseCase.createStation(station);
  }

  @PutMapping("/{stationId}")
  public Station updateStation(@PathVariable String stationId, @RequestBody Station station) {
    return updateStationUseCase.updateStation(stationId, station);
  }

  @DeleteMapping("/{stationId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteStation(@PathVariable String stationId) {
    deleteStationUseCase.deleteStation(stationId);
  }
}
