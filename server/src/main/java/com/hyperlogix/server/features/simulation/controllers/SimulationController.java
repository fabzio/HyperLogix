package com.hyperlogix.server.features.simulation.controllers;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.hyperlogix.server.features.simulation.dtos.SimulationCommandRequest;
import com.hyperlogix.server.features.simulation.dtos.SimulationRequest;
import com.hyperlogix.server.features.simulation.dtos.StartSimulationRequest;
import com.hyperlogix.server.features.simulation.usecases.StartSimulationUseCase;
import com.hyperlogix.server.features.simulation.usecases.StopSimulationUseCase;
import com.hyperlogix.server.features.simulation.usecases.in.StartSimulationUseCaseIn;

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
  public void startSimulation(@Payload StartSimulationRequest request, Principal principal) {
    StartSimulationUseCaseIn useCaseIn = new StartSimulationUseCaseIn(principal.getName(), request.getStartTimeOrders(),
        request.getEndTimeOrders());
    startSimulationUseCase.startSimulation(useCaseIn);
  }

  @MessageMapping("/simulation/stop")
  public void stopSimulation(Principal principal) {
    String simulationId = principal.getName();
    stopSimulationUseCase.stopSimulation(simulationId);
  }

  @MessageMapping("/simulation/command")
  public void sendCommand(@Payload SimulationCommandRequest request, Principal principal) {
    String simulationId = principal.getName();
    String command = request.getCommand();
    sendCommandUseCase.sendCommand(simulationId, command);
  }

  @MessageMapping("/simulation/subscribe")
  public void subscribe(@Payload SimulationRequest request, Principal principal) {
    String simulationId = request.getSimulationId();
    messagingTemplate.convertAndSend("/topic/simulation/" + simulationId, "Subscribed to simulation " + simulationId);
  }
}
