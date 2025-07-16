package com.hyperlogix.server.features.planification.usecases;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyperlogix.server.services.simulation.SimulationService;

@Service
public class SendLogisticCollapseUseCase {

  @Autowired
  private SimulationService simulationService;

  public void sendCollapseAlert(String simulationId, String collapseType, String description,
                               double severityLevel, String affectedArea) {
    simulationService.sendLogisticCollapseAlert(simulationId, collapseType, description,
                                               severityLevel, affectedArea);
  }
}
