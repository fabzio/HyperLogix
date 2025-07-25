package com.hyperlogix.server.services.simulation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;

import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.domain.OrderStatus;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.domain.Stop;
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
  public Map<String, RealTimeSimulationEngine> getRealTimeSimulationEngines() {
    return realTimeSimulation;
  }

  private final ApplicationEventPublisher eventPublisher;
  private final SimpMessagingTemplate messaging;
  private final Map<String, SimulationEngine> simulation = new ConcurrentHashMap<>();
  private final Map<String, RealTimeSimulationEngine> realTimeSimulation = new ConcurrentHashMap<>();
  private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
    Thread t = new Thread(r, "SimulationService-" + System.currentTimeMillis());
    t.setDaemon(true);
    return t;
  });

  // Campos para tracking y debugging
  private final AtomicInteger planificationCounter = new AtomicInteger(0);
  private final Map<String, LocalDateTime> lastPlanificationTime = new ConcurrentHashMap<>();
  private final Map<String, Integer> planificationFailureCount = new ConcurrentHashMap<>();

  @Autowired
  private PlanificationService planificationService;

  @Autowired
  private BlockadeProcessor blockadeProcessor;

  public SimulationService(SimpMessagingTemplate messaging, ApplicationEventPublisher eventPublisher) {
    this.messaging = messaging;
    this.eventPublisher = eventPublisher;
  }

  public void startSimulation(String simulationId, PLGNetwork network, String mode) {
    try {
      MDC.put("sessionId", simulationId);
      MDC.put("operation", "startSimulation");
      
      log.info("=== SIMULATION START REQUEST === ID: {}, Mode: {}, Timestamp: {}",
          simulationId, mode, LocalDateTime.now());
      
      // Log network state
      log.info("Network state - Trucks: {}, Orders: {}, Stations: {}",
          network.getTrucks().size(), network.getOrders().size(), 
          network.getStations() != null ? network.getStations().size() : 0);
      
      // Log order distribution by status
      Map<OrderStatus, Long> orderStatusCount = network.getOrders().stream()
          .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
      log.info("Order status distribution: {}", orderStatusCount);
      
      // Reset counters for this simulation
      planificationFailureCount.remove(simulationId);
      lastPlanificationTime.remove(simulationId);

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
      
      log.info("Simulation config - Resolution: {}, Algorithm Interval: {}, Acceleration: {}, Algorithm Time: {}",
          config.getSimulationResolution(), config.getCurrentAlgorithmInterval(),
          config.getTimeAcceleration(), config.getAlgorithmTime());

      SimulationNotifier notifier = snapshot -> {
        try {
          messaging.convertAndSend("/topic/simulation/" + simulationId, snapshot);
        } catch (Exception e) {
          log.error("Error sending snapshot for simulation {}: {}", simulationId, e.getMessage());
        }
      };
      
      List<Order> orderslist = new ArrayList<>(network.getOrders());
      stopSimulation(simulationId);
      
      SimulationEngine engine = new SimulationEngine(simulationId, config, notifier, orderslist,
          eventPublisher, planificationService, () -> {
            log.info("Simulation {} completed, removing from registry", simulationId);
            simulation.remove(simulationId);
            planificationFailureCount.remove(simulationId);
            lastPlanificationTime.remove(simulationId);
          });
      engine.setPlgNetwork(network);
      simulation.put(simulationId, engine);
      
      log.info("Submitting simulation {} to executor", simulationId);
      executor.execute(engine);
      
      log.info("=== SIMULATION STARTED === ID: {}", simulationId);
      
    } catch (Exception e) {
      log.error("FATAL ERROR starting simulation {}: {}", simulationId, e.getMessage(), e);
      throw e;
    } finally {
      MDC.clear();
    }
  }

  public void sendCommand(String simulationId, String command) {
    try {
      MDC.put("sessionId", simulationId);
      MDC.put("operation", "sendCommand");
      
      log.info("=== COMMAND RECEIVED === Simulation: {}, Command: {}, Timestamp: {}",
          simulationId, command, LocalDateTime.now());

      SimulationEngine engine = simulation.get(simulationId);
      if (engine != null) {
        log.info("Sending command '{}' to standard simulation engine: {}", command, simulationId);
        engine.handleCommand(command);
        return;
      }

      RealTimeSimulationEngine realTimeEngine = realTimeSimulation.get(simulationId);
      if (realTimeEngine != null) {
        log.info("Sending command '{}' to real-time simulation engine: {}", command, simulationId);
        realTimeEngine.handleCommand(command);
        return;
      }

      log.error("COMMAND FAILED - No simulation found with ID: {}. Available simulations: standard={}, realTime={}",
          simulationId, simulation.keySet(), realTimeSimulation.keySet());
          
    } catch (Exception e) {
      log.error("ERROR in sendCommand for simulation {} with command '{}': {}", 
          simulationId, command, e.getMessage(), e);
    } finally {
      MDC.clear();
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
    try {
      MDC.put("sessionId", simulationId);
      MDC.put("operation", "sendPlanification");
      
      int planificationNumber = planificationCounter.incrementAndGet();
      LocalDateTime now = LocalDateTime.now();
      
      log.info("=== PLANIFICATION #{} RECEIVED === Simulation: {}, Routes: {}, Timestamp: {}",
          planificationNumber, simulationId, route != null ? "VALID" : "NULL", now);
      
      if (route != null) {
        log.info("Route details - Trucks with routes: {}, Total stops: {}, Total paths: {}",
            route.getStops().size(),
            route.getStops().values().stream().mapToInt(List::size).sum(),
            route.getPaths().values().stream().mapToInt(List::size).sum());
        
        // Log detallado de cada ruta
        route.getStops().forEach((truckId, stops) -> {
          log.info("  Truck {}: {} stops", truckId, stops.size());
          if (log.isDebugEnabled()) {
            for (int i = 0; i < stops.size(); i++) {
              Stop stop = stops.get(i);
              log.debug("    Stop {}: ({}, {}) at {}", i,
                  stop.getNode().getLocation().x(), stop.getNode().getLocation().y(),
                  stop.getArrivalTime());
            }
          }
        });
      } else {
        log.error("NULL ROUTE received for simulation {}", simulationId);
        planificationFailureCount.merge(simulationId, 1, Integer::sum);
        
        int failures = planificationFailureCount.get(simulationId);
        log.error("Consecutive planification failures for {}: {}", simulationId, failures);
        
        if (failures >= 5) {
          log.error("CRITICAL: Too many planification failures for simulation {}", simulationId);
          sendLogisticCollapseAlert(simulationId, "PLANIFICATION_FAILURE", 
              "Multiple consecutive planification failures", 0.9, "System-wide");
        }
      }

      SimulationEngine engine = simulation.get(simulationId);
      if (engine != null) {
        log.info("Sending planification to standard engine for {}", simulationId);
        lastPlanificationTime.put(simulationId, now);
        
        if (route != null) {
          planificationFailureCount.remove(simulationId); // Reset failure count on success
        }
        
        engine.onPlanificationResult(route);
      } else {
        log.warn("No standard simulation engine found for {}", simulationId);
      }

      RealTimeSimulationEngine realTimeEngine = realTimeSimulation.get(simulationId);
      if (realTimeEngine != null) {
        log.info("Sending planification to real-time engine for {}", simulationId);
        lastPlanificationTime.put(simulationId, now);
        
        if (route != null) {
          planificationFailureCount.remove(simulationId); // Reset failure count on success
        }
        
        realTimeEngine.onPlanificationResult(route);
      } else {
        log.warn("No real-time simulation engine found for {}", simulationId);
      }
      
      if (engine == null && realTimeEngine == null) {
        log.error("NO SIMULATION ENGINE FOUND for ID: {}. Available: standard={}, realTime={}",
            simulationId, simulation.keySet(), realTimeSimulation.keySet());
      }
      
    } catch (Exception e) {
      log.error("ERROR in sendPlanification for simulation {}: {}", simulationId, e.getMessage(), e);
      
      // Log state for debugging
      log.error("Current simulations - Standard: {}, RealTime: {}", 
          simulation.keySet(), realTimeSimulation.keySet());
          
    } finally {
      MDC.clear();
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

  // Agregar método para monitorear planificaciones perdidas
  @Scheduled(fixedDelay = 30000) // Cada 30 segundos
  public void monitorPlanifications() {
    LocalDateTime now = LocalDateTime.now();
    
    lastPlanificationTime.forEach((simulationId, lastTime) -> {
      Duration timeSinceLastPlan = Duration.between(lastTime, now);
      
      if (timeSinceLastPlan.compareTo(Duration.ofMinutes(2)) > 0) {
        log.warn("STALE PLANIFICATION WARNING: Simulation {} hasn't received planification for {} minutes",
            simulationId, timeSinceLastPlan.toMinutes());
            
        // Check if simulation is still running
        if (simulation.containsKey(simulationId) || realTimeSimulation.containsKey(simulationId)) {
          log.warn("Simulation {} is still active but planifications are stale", simulationId);
        }
      }
    });
  }

  public void sendLogisticCollapseAlert(String simulationId, String collapseType, String description,
      double severityLevel, String affectedArea) {
    log.error("=== LOGISTIC COLLAPSE ALERT === Simulation: {}, Type: {}, Severity: {}, Area: {}, Description: {}",
        simulationId, collapseType, severityLevel, affectedArea, description);

    // Create collapse alert message
    var collapseAlert = new java.util.HashMap<String, Object>();
    collapseAlert.put("type", "logistic_collapse");
    collapseAlert.put("collapseType", collapseType);
    collapseAlert.put("description", description);
    collapseAlert.put("severityLevel", severityLevel);
    collapseAlert.put("affectedArea", affectedArea);
    collapseAlert.put("timestamp", LocalDateTime.now());

    try {
      // Send alert to client via WebSocket
      messaging.convertAndSend("/topic/simulation/" + simulationId + "/alerts", collapseAlert);
      log.info("Collapse alert sent successfully for simulation {}", simulationId);
    } catch (Exception e) {
      log.error("Failed to send collapse alert for simulation {}: {}", simulationId, e.getMessage(), e);
    }
  }
}











