package com.hyperlogix.server.features.simulation.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.GetMapping;

import com.hyperlogix.server.features.simulation.dtos.StartSimulationRequest;
import com.hyperlogix.server.features.simulation.dtos.SimulationCommandRequest;
import com.hyperlogix.server.features.simulation.usecases.StartSimulationUseCase;
import com.hyperlogix.server.features.simulation.usecases.in.StartSimulationUseCaseIn;
import com.hyperlogix.server.services.simulation.SimulationService;
import com.hyperlogix.server.services.simulation.SimulationStatus;

@RestController
@RequestMapping("/simulation")
public class SimulationController {

  @Autowired
  private StartSimulationUseCase startSimulationUseCase;

  @Autowired
  private SimulationService simulationService;

  @PostMapping("/start/{simulationId}")
  public ResponseEntity<Void> startSimulation(
      @PathVariable String simulationId,
      @RequestBody StartSimulationRequest request) {

    StartSimulationUseCaseIn useCaseIn = new StartSimulationUseCaseIn(
        simulationId,
        request.getStartTimeOrders(),
        request.getEndTimeOrders());
    startSimulationUseCase.startSimulation(useCaseIn);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/status/{simulationId}")
  public ResponseEntity<SimulationStatus> getSimulationStatus(@PathVariable String simulationId) {
    SimulationStatus status = simulationService.getSimulationStatus(simulationId);
    if (status != null) {
      return ResponseEntity.ok(status);
    }
    return ResponseEntity.notFound().build();
  }

  @PostMapping("/stop/{simulationId}")
  public ResponseEntity<Void> stopSimulation(@PathVariable String simulationId) {
    simulationService.stopSimulation(simulationId);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/command/{simulationId}")
  public ResponseEntity<Void> sendCommand(
      @PathVariable String simulationId,
      @RequestBody SimulationCommandRequest commandRequest) {
    simulationService.sendCommand(simulationId, commandRequest.getCommand());
    return ResponseEntity.ok().build();
  }

}
