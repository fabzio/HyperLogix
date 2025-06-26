package com.hyperlogix.server.services.simulation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.services.planification.PlanificationService;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class SimulationService {
  private final ApplicationEventPublisher eventPublisher;
  private final SimpMessagingTemplate messaging;
  private final Map<String, SimulationEngine> simulation = new ConcurrentHashMap<>();
  private final Map<String, RealTimeSimulationEngine> realTimeSimulation = new ConcurrentHashMap<>();
  private final ExecutorService executor = Executors.newCachedThreadPool();

  @Autowired
  private PlanificationService planificationService;

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
          Duration.ofMillis(100));
    } else {
      config = new SimulationConfig(
          Duration.ofSeconds(3),
          Duration.ofSeconds(5),
          256.0,
          Duration.ofMillis(100));
    }
    SimulationNotifier notifier = snapshot -> {
      messaging.convertAndSend("/topic/simulation/" + simulationId, snapshot);
    };
    List<Order> orderslist = new ArrayList<>(network.getOrders());
    stopSimulation(simulationId);
    SimulationEngine engine = new SimulationEngine(simulationId, config, notifier, orderslist,
        eventPublisher, planificationService);
    engine.setPlgNetwork(network);
    simulation.put(simulationId, engine);
    executor.execute(engine);
  }

  public void sendCommand(String simulationId, String command) {
    SimulationEngine engine = simulation.get(simulationId);
    if (engine != null) {
      engine.handleCommand(command);
    }
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

    return new SimulationStatus(false, false, 0);
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
        realTimeOrderRepository, eventPublisher, planificationService);
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
}
