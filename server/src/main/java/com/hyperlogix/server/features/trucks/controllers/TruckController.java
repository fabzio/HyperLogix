package com.hyperlogix.server.features.trucks.controllers;

import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.features.trucks.usecases.CreateTruckUseCase;
import com.hyperlogix.server.features.trucks.usecases.DeleteTruckUseCase;
import com.hyperlogix.server.features.trucks.usecases.GetTruckUseCase;
import com.hyperlogix.server.features.trucks.usecases.GetTrucksUseCase;
import com.hyperlogix.server.features.trucks.usecases.UpdateTruckUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trucks")
public class TruckController {
  private final GetTrucksUseCase getTrucksUseCase;
  private final GetTruckUseCase getTruckUseCase;
  private final CreateTruckUseCase createTruckUseCase;
  private final UpdateTruckUseCase updateTruckUseCase;
  private final DeleteTruckUseCase deleteTruckUseCase;

  public TruckController(
      GetTrucksUseCase getTrucksUseCase, 
      GetTruckUseCase getTruckUseCase,
      CreateTruckUseCase createTruckUseCase,
      UpdateTruckUseCase updateTruckUseCase,
      DeleteTruckUseCase deleteTruckUseCase
  ) {
    this.getTrucksUseCase = getTrucksUseCase;
    this.getTruckUseCase = getTruckUseCase;
    this.createTruckUseCase = createTruckUseCase;
    this.updateTruckUseCase = updateTruckUseCase;
    this.deleteTruckUseCase = deleteTruckUseCase;
  }

  @GetMapping
  public Page<Truck> list(@PageableDefault(size = 10) Pageable pageable) {
    return getTrucksUseCase.getAllTrucks(pageable);
  }

  @GetMapping("/truck/{truckId}")
  public Truck getTruckById(@PathVariable String truckId) {
    return getTruckUseCase.getTruckById(truckId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Truck createTruck(@RequestBody Truck truck) {
    return createTruckUseCase.createTruck(truck);
  }

  @PutMapping("/{truckId}")
  public Truck updateTruck(@PathVariable String truckId, @RequestBody Truck truck) {
    return updateTruckUseCase.updateTruck(truckId, truck);
  }

  @DeleteMapping("/{truckId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteTruck(@PathVariable String truckId) {
    deleteTruckUseCase.deleteTruck(truckId);
  }
}
