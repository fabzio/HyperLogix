package com.hyperlogix.server.features.planification.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;

import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.features.planification.usecases.SendPlanificationUseCase;
import com.hyperlogix.server.features.planification.usecases.GeneratePlanificationUseCase;
import com.hyperlogix.server.features.planification.dtos.PlanificationRequest;

public class PlanificationController {

  @Autowired
  private GeneratePlanificationUseCase generatePlanificationUseCase;
  @Autowired
  private SendPlanificationUseCase sendPlanificationUseCase;

  @MessageMapping("/planification/request")
  public void handlePlanificationRequest(PlanificationRequest request) {
    generatePlanificationUseCase.generateRoutes(request.getSessionId(), request.getPlgNetwork());
  }

  @MessageMapping("/planification/response")
  public void handlePlanificationResponse(Routes response) {
    // TODO: use a simulation ID instead of hardcoded "A"
    sendPlanificationUseCase.sendPlanification("A", response);
  }

}
