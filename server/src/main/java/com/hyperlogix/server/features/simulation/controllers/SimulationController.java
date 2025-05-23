package com.hyperlogix.server.features.simulation.controllers;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.hyperlogix.server.features.simulation.dtos.SimulationCommandRequest;
import com.hyperlogix.server.features.simulation.dtos.SimulationRequest;
import com.hyperlogix.server.features.simulation.usecases.StartSimulationUseCase;
import com.hyperlogix.server.features.simulation.usecases.StopSimulationUseCase;
import com.hyperlogix.server.features.simulation.usecases.SendCommandUseCase;

@Controller
public class SimulationController {
  @Autowired
  private SimpMessagingTemplate messagingTemplate;
  @Autowired
  private StartSimulationUseCase startSimulationUseCase;
  @Autowired
  private StopSimulationUseCase stopSimulationUseCase;
  @Autowired
  private SendCommandUseCase sendCommandUseCase;

  @MessageMapping("/simulation/start")
  public void startSimulation(@Payload SimulationRequest request, Principal principal) {
    String simulationId = request.getSimulationId();
    startSimulationUseCase.startSimulation(simulationId);
  }

  @MessageMapping("/simulation/stop")
  public void stopSimulation(@Payload SimulationRequest request, Principal principal) {
    String simulationId = request.getSimulationId();
    stopSimulationUseCase.stopSimulation(simulationId);
  }

  @MessageMapping("/simulation/command")
  public void sendCommand(@Payload SimulationCommandRequest request, Principal principal) {
    String simulationId = request.getSimulationId();
    String command = request.getCommand();
    sendCommandUseCase.sendCommand(simulationId, command);
  }

  @MessageMapping("/simulation/subscribe")
  public void subscribe(@Payload SimulationRequest request, Principal principal) {
    String simulationId = request.getSimulationId();
    messagingTemplate.convertAndSend("/topic/simulation/" + simulationId, "Subscribed to simulation " + simulationId);
  }
}
