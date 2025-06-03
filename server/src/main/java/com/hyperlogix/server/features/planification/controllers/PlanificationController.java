package com.hyperlogix.server.features.planification.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.hyperlogix.server.features.planification.usecases.SendPlanificationUseCase;
import com.hyperlogix.server.features.planification.usecases.GeneratePlanificationUseCase;
import com.hyperlogix.server.features.planification.dtos.PlanificationRequestEvent;
import com.hyperlogix.server.features.planification.dtos.PlanificationResponseEvent;

@Controller
public class PlanificationController {

  @Autowired
  private GeneratePlanificationUseCase generatePlanificationUseCase;
  @Autowired
  private SendPlanificationUseCase sendPlanificationUseCase;

  @EventListener
  public void handlePlanificationRequest(PlanificationRequestEvent request) {
    generatePlanificationUseCase.generateRoutes(request.getSessionId(), request.getPlgNetwork(),
        request.getSimulatedTime());
  }

  @EventListener
  public void handlePlanificationResponse(PlanificationResponseEvent response) {
    sendPlanificationUseCase.sendPlanification(response.getSessionId(), response.getRoutes());
  }

}
