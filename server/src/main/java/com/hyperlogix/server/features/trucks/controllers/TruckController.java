package com.hyperlogix.server.features.trucks.controllers;

import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.features.trucks.usecases.GetTrucksUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trucks")
public class TruckController {
  private final GetTrucksUseCase getTrucksUseCase;

  public TruckController(GetTrucksUseCase getTrucksUseCase) {
    this.getTrucksUseCase = getTrucksUseCase;
  }

  @GetMapping
  public Page<Truck> list(@PageableDefault(size = 10) Pageable pageable) {
    return getTrucksUseCase.getAllTrucks(pageable);
  }
}
