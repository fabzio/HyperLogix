package com.hyperlogix.server.services.planification;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.features.planification.dtos.PlanificationResponseEvent;

@Service
public class PlanificationService {
  @Autowired
  private ApplicationEventPublisher eventPublisher;
  @Autowired
  private SimpMessagingTemplate messaging;
  private final Map<String, PlanificationEngine> planification = new ConcurrentHashMap<>();
  private final ExecutorService executor = Executors.newFixedThreadPool(8);

  public void startPlanification(String planificationId, PLGNetwork network, LocalDateTime algorithmTime,
      Duration algorithmDuration) {
    PlanificationNotifier notifier = routes -> {
      PlanificationResponseEvent responseEvent = new PlanificationResponseEvent(planificationId, routes);
      messaging.convertAndSend("/topic/planification/response",
          responseEvent);
      eventPublisher.publishEvent(responseEvent);
    };
    PlanificationEngine engine = new PlanificationEngine(network, notifier, algorithmTime, algorithmDuration);
    stopPlanification(planificationId);
    planification.put(planificationId, engine);
    executor.execute(engine);
  }

  public void stopPlanification(String planificationId) {
    PlanificationEngine engine = planification.get(planificationId);
    if (engine != null) {
      engine.stop();
      planification.remove(planificationId);
    }
  }

  public PlanificationStatus getPlanificationStatus(String planificationId) {
    PlanificationEngine engine = planification.get(planificationId);
    if (engine != null) {
      return engine.getStatus();
    }
    return new PlanificationStatus(false, 0);
  }
}