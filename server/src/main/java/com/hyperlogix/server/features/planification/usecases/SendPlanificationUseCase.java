package com.hyperlogix.server.features.planification.usecases;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.services.simulation.SimulationService;

@Service
public class SendPlanificationUseCase {

  @Autowired
  private SimulationService simulationService;

  public void sendPlanification(String orderId, Routes routes) {
    simulationService.sendPlanification(orderId, routes);
  }
}
