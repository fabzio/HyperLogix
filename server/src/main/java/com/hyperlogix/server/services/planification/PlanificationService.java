package com.hyperlogix.server.services.planification;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.hyperlogix.server.domain.PLGNetwork;

@Service
public class PlanificationService {
  @Autowired
  private SimpMessagingTemplate messaging;
  private final Map<String, PlanificationEngine> planification = new ConcurrentHashMap<>();
  private final ExecutorService executor = Executors.newCachedThreadPool();

  public void startPlanification(String planificationId, PLGNetwork network) {
    PlanificationNotifier notifier = routes -> {
      messaging.convertAndSend("/topic/planification/" + planificationId, routes);
    };
    PlanificationEngine engine = new PlanificationEngine(network, notifier);
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
}