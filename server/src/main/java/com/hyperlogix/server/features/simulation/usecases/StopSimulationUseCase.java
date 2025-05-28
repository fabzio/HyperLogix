package com.hyperlogix.server.features.simulation.usecases;

import org.springframework.stereotype.Service;

import com.hyperlogix.server.services.simulation.SimulationService;

@Service
public class StopSimulationUseCase {
  private final SimulationService simulationService;

  public StopSimulationUseCase(SimulationService simulationService) {
    this.simulationService = simulationService;
  }

  public void stopSimulation(String simulationId) {
    simulationService.stopSimulation(simulationId);
  }
}
