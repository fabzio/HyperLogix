package com.hyperlogix.server.services.simulation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.services.planification.PlanificationService;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SimulationService {
  private final ApplicationEventPublisher eventPublisher;
  private final SimpMessagingTemplate messaging;
  private final Map<String, SimulationEngine> simulation = new ConcurrentHashMap<>();
  private final Map<String, RealTimeSimulationEngine> realTimeSimulation = new ConcurrentHashMap<>();
  private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
    Thread t = new Thread(r, "SimulationService-" + System.currentTimeMillis());
    t.setDaemon(true);
    return t;
  });

  @Autowired
  private PlanificationService planificationService;

  @Autowired
  private BlockadeProcessor blockadeProcessor;

  public SimulationService(SimpMessagingTemplate messaging, ApplicationEventPublisher eventPublisher) {
    this.messaging = messaging;
    this.eventPublisher = eventPublisher;
  }

  public void startSimulation(String simulationId, PLGNetwork network, String mode) {
    SimulationConfig config;

    if ("real".equals(mode)) {
      config = new SimulationConfig(
          Duration.ofSeconds(3),
          Duration.ofSeconds(5),
          1.0,
          Duration.ofMillis(300));
    } else {
      config = new SimulationConfig(
          Duration.ofSeconds(3),
          Duration.ofSeconds(5),
          256.0,  // Mantener la aceleración original
          Duration.ofMillis(300));
    }
    SimulationNotifier notifier = snapshot -> {
      messaging.convertAndSend("/topic/simulation/" + simulationId, snapshot);
    };
    List<Order> orderslist = new ArrayList<>(network.getOrders());
    stopSimulation(simulationId);
    SimulationEngine engine = new SimulationEngine(simulationId, config, notifier, orderslist,
        eventPublisher, planificationService, () -> {
          System.out.println("Removing");
          simulation.remove(simulationId);
        });
    engine.setPlgNetwork(network);
    simulation.put(simulationId, engine);
    executor.execute(engine);
  }

  public void sendCommand(String simulationId, String command) {
    log.info("Sending command '{}' to simulation '{}'", command, simulationId);

    SimulationEngine engine = simulation.get(simulationId);
    if (engine != null) {
      log.info("Found standard simulation engine for ID: {}", simulationId);
      engine.handleCommand(command);
      return;
    }

    RealTimeSimulationEngine realTimeEngine = realTimeSimulation.get(simulationId);
    if (realTimeEngine != null) {
      log.info("Found real-time simulation engine for ID: {}", simulationId);
      realTimeEngine.handleCommand(command);
      return;
    }

    log.warn("No simulation found with ID: {}. Available simulations: standard={}, realTime={}",
        simulationId, simulation.keySet(), realTimeSimulation.keySet());
  }

  public void stopSimulation(String simulationId) {
    SimulationEngine engine = simulation.get(simulationId);
    if (engine != null) {
      engine.stop();
      simulation.remove(simulationId);
    }

    RealTimeSimulationEngine realTimeEngine = realTimeSimulation.get(simulationId);
    if (realTimeEngine != null) {
      realTimeEngine.stop();
      realTimeSimulation.remove(simulationId);
    }
  }

  public void sendPlanification(String simulationId, Routes route) {
    System.out.println("Sending planification " + simulationId + " for simulation: " + route);
    SimulationEngine engine = simulation.get(simulationId);
    if (engine != null) {
      engine.onPlanificationResult(route);
    }

    RealTimeSimulationEngine realTimeEngine = realTimeSimulation.get(simulationId);
    if (realTimeEngine != null) {
      realTimeEngine.onPlanificationResult(route);
    }
  }

  public SimulationStatus getSimulationStatus(String simulationId) {
    SimulationEngine engine = simulation.get(simulationId);
    if (engine != null) {
      return engine.getStatus();
    }

    RealTimeSimulationEngine realTimeEngine = realTimeSimulation.get(simulationId);
    if (realTimeEngine != null) {
      return realTimeEngine.getStatus();
    }

    return new SimulationStatus(false, false, 1.0);
  }

  public void startRealTimeSimulation(String simulationId, PLGNetwork network,
      com.hyperlogix.server.features.operation.repository.RealTimeOrderRepository realTimeOrderRepository) {
    SimulationConfig config = new SimulationConfig(
        Duration.ofSeconds(3),
        Duration.ofSeconds(5),
        1.0, // Real-time acceleration (1:1)
        Duration.ofMillis(1000));

    SimulationNotifier notifier = snapshot -> {
      messaging.convertAndSend("/topic/simulation/" + simulationId, snapshot);
    };

    // Use the real-time order repository instead of a static list
    stopSimulation(simulationId);
    RealTimeSimulationEngine engine = new RealTimeSimulationEngine(simulationId, config, notifier,
        realTimeOrderRepository, eventPublisher, planificationService, blockadeProcessor);
    engine.setPlgNetwork(network);
    realTimeSimulation.put(simulationId, engine);
    executor.execute(engine);
  }

  public void updateTruckState(String simulationId, String truckId, com.hyperlogix.server.domain.TruckState newState) {
    RealTimeSimulationEngine engine = realTimeSimulation.get(simulationId);
    if (engine != null) {
      engine.updateTruckState(truckId, newState);
    }
  }

  /**
   * Triggers an immediate notification for real-time simulation.
   * Useful when orders are added and we want immediate updates.
   */
  public void triggerImmediateUpdate(String simulationId) {
    RealTimeSimulationEngine engine = realTimeSimulation.get(simulationId);
    if (engine != null) {
      engine.triggerImmediateUpdate();
    }
  }

  /**
   * Triggers immediate planification for real-time simulation.
   * Used when new orders arrive to ensure quick processing.
   */
  public void triggerImmediatePlanification(String simulationId) {
    RealTimeSimulationEngine engine = realTimeSimulation.get(simulationId);
    if (engine != null) {
      engine.triggerImmediatePlanification();
    }
  }

  /**
   * Schedules a future maintenance for a truck in real-time simulation.
   * 
   * @param simulationId    The simulation ID
   * @param truckId         The truck ID
   * @param maintenanceTime The time when maintenance should occur
   */
  public void scheduleTruckMaintenance(String simulationId, String truckId, java.time.LocalDateTime maintenanceTime) {
    RealTimeSimulationEngine engine = realTimeSimulation.get(simulationId);
    if (engine != null) {
      engine.scheduleTruckMaintenance(truckId, maintenanceTime);
      log.info("Scheduled maintenance for truck {} at {} in simulation {}", truckId, maintenanceTime, simulationId);
    } else {
      log.warn("No real-time simulation found with ID: {} when trying to schedule maintenance", simulationId);
    }
  }

  /**
   * Cleanup resources when the service is being destroyed
   */
  @PreDestroy
  public void cleanup() {
    log.info("Cleaning up SimulationService resources...");

    // Stop all running simulations
    simulation.values().forEach(SimulationEngine::stop);
    realTimeSimulation.values().forEach(RealTimeSimulationEngine::stop);

    // Clear the maps
    simulation.clear();
    realTimeSimulation.clear();

    // Shutdown executor service
    executor.shutdown();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        log.warn("Executor did not terminate gracefully, forcing shutdown");
        executor.shutdownNow();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          log.error("Executor did not terminate after forced shutdown");
        }
      }
    } catch (InterruptedException e) {
      log.warn("Interrupted while waiting for executor termination");
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    log.info("SimulationService cleanup completed");
  }

  public void sendLogisticCollapseAlert(String simulationId, String collapseType, String description,
      double severityLevel, String affectedArea) {
    System.out.println("Sending logistic collapse alert for simulation: " + simulationId);

    // Create collapse alert message
    var collapseAlert = new java.util.HashMap<String, Object>();
    collapseAlert.put("type", "logistic_collapse");
    collapseAlert.put("collapseType", collapseType);
    collapseAlert.put("description", description);
    collapseAlert.put("severityLevel", severityLevel);
    collapseAlert.put("affectedArea", affectedArea);
    collapseAlert.put("timestamp", java.time.LocalDateTime.now());

    // Send alert to client via WebSocket
    messaging.convertAndSend("/topic/simulation/" + simulationId + "/alerts", collapseAlert);
  }
}











