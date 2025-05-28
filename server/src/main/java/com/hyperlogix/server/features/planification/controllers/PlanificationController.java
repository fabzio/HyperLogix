package com.hyperlogix.server.features.planification.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;

import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.features.planification.usecases.SendPlanificationUseCase;

public class PlanificationController {

  @Autowired
  private SendPlanificationUseCase sendPlanificationUseCase;

  @MessageMapping("/planification/response")
  public void handlePlanificationResponse(Routes response) {
    sendPlanificationUseCase.sendPlanification("A", response);

  }
}
