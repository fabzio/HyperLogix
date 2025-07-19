package com.hyperlogix.server.features.planification.usecases;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Incident;
import com.hyperlogix.server.services.planification.PlanificationService;

@Service
public class GeneratePlanificationUseCase {

  @Autowired
  private PlanificationService planificationService;

    public void generateRoutes(String sessionId, PLGNetwork network, LocalDateTime algorithmTime,
      Duration algorithmDuration) {
    planificationService.startPlanification(sessionId, network, algorithmTime, algorithmDuration, List.of());
  }

  public void generateRoutes(String sessionId, PLGNetwork network, LocalDateTime algorithmTime,
      Duration algorithmDuration, List<Incident> incidents) {
    planificationService.startPlanification(sessionId, network, algorithmTime, algorithmDuration, incidents);
  }
}