package com.hyperlogix.server.features.planification.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;

import com.hyperlogix.server.features.planification.usecases.SendPlanificationUseCase;
import com.hyperlogix.server.features.planification.usecases.GeneratePlanificationUseCase;
import com.hyperlogix.server.features.planification.dtos.PlanificationRequest;
import com.hyperlogix.server.features.planification.dtos.PlanificationResponse;

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
  public void handlePlanificationResponse(PlanificationResponse response) {
    sendPlanificationUseCase.sendPlanification(response.getSessionId(), response.getRoutes());
  }

}
