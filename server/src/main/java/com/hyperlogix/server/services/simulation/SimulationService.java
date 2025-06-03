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

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class SimulationService {
  private final ApplicationEventPublisher eventPublisher;
  private final SimpMessagingTemplate messaging;
  private final Map<String, SimulationEngine> simulation = new ConcurrentHashMap<>();
  private final ExecutorService executor = Executors.newCachedThreadPool();

  public SimulationService(SimpMessagingTemplate messaging, ApplicationEventPublisher eventPublisher) {
    this.messaging = messaging;
    this.eventPublisher = eventPublisher;
  }

  public void startSimulation(String simulationId, PLGNetwork network) {
    SimulationConfig config = new SimulationConfig(
        Duration.ofSeconds(3),
        Duration.ofSeconds(5),
        60,
        Duration.ofMillis(250));
    SimulationNotifier notifier = snapshot -> {
      messaging.convertAndSend("/topic/simulation/" + simulationId, snapshot);
    };
    List<Order> orderslist = new ArrayList<>(network.getOrders());
    stopSimulation(simulationId);
    SimulationEngine engine = new SimulationEngine(simulationId, config, notifier, orderslist,
        eventPublisher);
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
  }

  public void sendPlanification(String simulationId, Routes route) {
    System.out.println("Sending planification for simulation: " + route);
    SimulationEngine engine = simulation.get(simulationId);
    if (engine != null) {
      engine.onPlanificationResult(route);
    }
  }

  public SimulationStatus getSimulationStatus(String simulationId) {
    SimulationEngine engine = simulation.get(simulationId);
    if (engine != null) {
      return engine.getStatus();
    }
    return new SimulationStatus(false, false, 0);
  }
}
