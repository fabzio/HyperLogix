package com.hyperlogix.server.features.planification.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

import com.hyperlogix.server.features.planification.dtos.PlanificationRequestEvent;
import com.hyperlogix.server.features.planification.dtos.PlanificationResponseEvent;
import com.hyperlogix.server.features.planification.usecases.GeneratePlanificationUseCase;
import com.hyperlogix.server.features.planification.usecases.SendPlanificationUseCase;

@Controller
public class PlanificationController {

  @Autowired
  private GeneratePlanificationUseCase generatePlanificationUseCase;
  @Autowired
  private SendPlanificationUseCase sendPlanificationUseCase;

  @EventListener
    public void handlePlanificationRequest(PlanificationRequestEvent request) {
        generatePlanificationUseCase.generateRoutes(
            request.getSessionId(), 
            request.getPlgNetwork(),
            request.getSimulatedTime(), 
            request.getAlgorithmDuration(),
            request.getCompletedIncidents()
        );
    }

    @EventListener
    public void handlePlanificationResponse(PlanificationResponseEvent response) {
        sendPlanificationUseCase.sendPlanification(
            response.getSessionId(), 
            response.getRoutes(),
            response.getNewIncidents()
        );
    }
}
