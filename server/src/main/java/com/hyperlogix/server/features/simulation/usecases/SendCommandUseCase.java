package com.hyperlogix.server.features.simulation.usecases;

import org.springframework.stereotype.Service;

import com.hyperlogix.server.services.simulation.SimulationService;

@Service
public class SendCommandUseCase {
  private final SimulationService simulationService;

  public SendCommandUseCase(SimulationService simulationService) {
    this.simulationService = simulationService;
  }

  public void sendCommand(String simulationId, String command) {
    simulationService.sendCommand(simulationId, command);
  }
}
