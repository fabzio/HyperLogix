package com.hyperlogix.server.features.stations.controllers;

import com.hyperlogix.server.features.stations.usecases.GetStationsUseCase;
import com.hyperlogix.server.domain.Station;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/stations")
public class StationController {
  private final GetStationsUseCase getStationsUseCase;

  public StationController(GetStationsUseCase getStationsUseCase) {
    this.getStationsUseCase = getStationsUseCase;
  }

  @GetMapping
  public Page<Station> list(@PageableDefault(size = 20) Pageable pageable) {
    return getStationsUseCase.getAllStations(pageable);
  }
}
