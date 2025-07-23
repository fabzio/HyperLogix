package com.hyperlogix.server.services.planification;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.hyperlogix.server.domain.Incident;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.features.planification.dtos.PlanificationResponseEvent;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PlanificationService {
  @Autowired
  private ApplicationEventPublisher eventPublisher;
  @Autowired
  private SimpMessagingTemplate messaging;
  private final Map<String, PlanificationEngine> planification = new ConcurrentHashMap<>();
  private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
    Thread t = new Thread(r, "PlanificationService-" + System.currentTimeMillis());
    t.setDaemon(true);
    return t;
  });
private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  public void startPlanification(String planificationId, PLGNetwork network, LocalDateTime algorithmTime,
      Duration algorithmDuration) {
    startPlanification(planificationId, network, algorithmTime, algorithmDuration, List.of());
  }

  public void startPlanification(String planificationId, PLGNetwork network, LocalDateTime algorithmTime,
      Duration algorithmDuration, List<Incident> incidents) {
    PlanificationNotifier notifier = routes -> {
      PlanificationResponseEvent responseEvent = new PlanificationResponseEvent(planificationId, routes);
      messaging.convertAndSend("/topic/planification/response",
          responseEvent);
      eventPublisher.publishEvent(responseEvent);
    };

    // Usar el nuevo constructor que incluye eventPublisher y sessionId
    PlanificationEngine engine = new PlanificationEngine(network, notifier, algorithmTime, algorithmDuration,
        incidents, eventPublisher, planificationId, () -> {
          System.out.println("Removing");
          planification.remove(planificationId);
        });
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

  /**
   * Cleanup resources when the service is being destroyed
   */
  @PreDestroy
  public void cleanup() {
    log.info("Cleaning up PlanificationService resources...");

    // Stop all running planifications
    planification.values().forEach(PlanificationEngine::stop);
    planification.clear();

    // Shutdown executor service
    executor.shutdown();
    try {
      if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
        log.warn("Executor did not terminate gracefully, forcing shutdown");
        executor.shutdownNow();
        if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
          log.error("Executor did not terminate after forced shutdown");
        }
      }
    } catch (InterruptedException e) {
      log.warn("Interrupted while waiting for executor termination");
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    // Shutdown scheduler
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(15, TimeUnit.SECONDS)) {
        log.warn("Scheduler did not terminate gracefully, forcing shutdown");
        scheduler.shutdownNow();
        if (!scheduler.awaitTermination(15, TimeUnit.SECONDS)) {
          log.error("Scheduler did not terminate after forced shutdown");
        }
      }
    } catch (InterruptedException e) {
      log.warn("Interrupted while waiting for scheduler termination");
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }

    log.info("PlanificationService cleanup completed");
  }
}