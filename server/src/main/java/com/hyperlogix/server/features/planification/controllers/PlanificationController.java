package com.hyperlogix.server.features.planification.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

import com.hyperlogix.server.features.planification.usecases.SendPlanificationUseCase;
import com.hyperlogix.server.features.planification.usecases.GeneratePlanificationUseCase;
import com.hyperlogix.server.features.planification.dtos.PlanificationRequestEvent;
import com.hyperlogix.server.features.planification.dtos.PlanificationResponseEvent;
import com.hyperlogix.server.features.planification.usecases.SendLogisticCollapseUseCase;
import com.hyperlogix.server.features.planification.dtos.LogisticCollapseEvent;

@Controller
public class PlanificationController {

  @Autowired
  private GeneratePlanificationUseCase generatePlanificationUseCase;
  @Autowired
  private SendPlanificationUseCase sendPlanificationUseCase;
  @Autowired
  private SendLogisticCollapseUseCase sendLogisticCollapseUseCase;

  @EventListener
  public void handlePlanificationRequest(PlanificationRequestEvent request) {
    generatePlanificationUseCase.generateRoutes(request.getSessionId(), request.getPlgNetwork(),
        request.getSimulatedTime(), request.getAlgorithmDuration());
  }

  @EventListener
  public void handlePlanificationResponse(PlanificationResponseEvent response) {
    sendPlanificationUseCase.sendPlanification(response.getSessionId(), response.getRoutes());
  }

  @EventListener
  public void handleLogisticCollapseEvent(LogisticCollapseEvent collapseEvent) {
    sendLogisticCollapseUseCase.sendCollapseAlert(
        collapseEvent.getSessionId(),
        collapseEvent.getCollapseType(),
        collapseEvent.getDescription(),
        collapseEvent.getSeverityLevel(),
        collapseEvent.getAffectedArea()
    );
  }
}
