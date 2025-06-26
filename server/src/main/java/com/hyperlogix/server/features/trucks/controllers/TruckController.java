package com.hyperlogix.server.features.trucks.controllers;

import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.features.trucks.usecases.GetTruckUseCase;
import com.hyperlogix.server.features.trucks.usecases.GetTrucksUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trucks")
public class TruckController {
  private final GetTrucksUseCase getTrucksUseCase;
  private final GetTruckUseCase getTruckUseCase;

  public TruckController(GetTrucksUseCase getTrucksUseCase, GetTruckUseCase getTruckUseCase) {
    this.getTruckUseCase = getTruckUseCase;
    this.getTrucksUseCase = getTrucksUseCase;
  }

  @GetMapping
  public Page<Truck> list(@PageableDefault(size = 10) Pageable pageable) {
    return getTrucksUseCase.getAllTrucks(pageable);
  }

  @GetMapping("/truck/{truckId}")
  public Truck getTruckById(@PathVariable String truckId) {
    return getTruckUseCase.getTruckById(truckId);
  }
}
