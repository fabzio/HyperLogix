package com.hyperlogix.server.features.simulation.usecases;

import org.springframework.stereotype.Service;

import com.hyperlogix.server.services.simulation.SimulationService;

@Service
public class StartSimulationUseCase {
  private final SimulationService simulationService;

  public StartSimulationUseCase(SimulationService simulationService) {
    this.simulationService = simulationService;
  }

  public void startSimulation(String simulationId) {
    simulationService.startSimulation(simulationId);
  }
}
