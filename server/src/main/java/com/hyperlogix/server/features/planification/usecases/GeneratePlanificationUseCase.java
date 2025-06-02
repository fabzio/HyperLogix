package com.hyperlogix.server.features.planification.usecases;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.services.planification.PlanificationService;

@Service
public class GeneratePlanificationUseCase {

  @Autowired
  private PlanificationService planificationService;

  public void generateRoutes(String sessionId, PLGNetwork network) {
    planificationService.startPlanification(sessionId, network);
  }
}