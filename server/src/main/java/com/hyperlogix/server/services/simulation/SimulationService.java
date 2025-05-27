package com.hyperlogix.server.services.simulation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.features.orders.controllers.OrderController;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class SimulationService {

  private final SimpMessagingTemplate messaging;
  private final Map<String, SimulationEngine> simulation = new ConcurrentHashMap<>();
  private final ExecutorService executor = Executors.newCachedThreadPool();

  public SimulationService(SimpMessagingTemplate messaging, OrderController orderController) {
    this.messaging = messaging;
  }

  public void startSimulation(String simulationId,List<Order> orders) {
    SimulationConfig config = new SimulationConfig(
        Duration.ofSeconds(3),
        Duration.ofSeconds(10),
        5,
        Duration.ofMillis(500)
    );
    SimulationNotifier notifier = snapshot -> {
      messaging.convertAndSend("/topic/simulation/" + simulationId, snapshot);
    };
    List<Order> orderslist = new ArrayList<>(orders);
    stopSimulation(simulationId);
    SimulationEngine engine = new SimulationEngine(simulationId, config, notifier, orderslist);
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
    SimulationEngine engine = simulation.get(simulationId);
    if (engine != null) {
      engine.onPlanificationResult(route);
    }
  }
}
