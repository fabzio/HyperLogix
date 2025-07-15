package com.hyperlogix.server.services.planification;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hyperlogix.server.domain.CompletedIncident;
import com.hyperlogix.server.domain.Incident;
import com.hyperlogix.server.domain.OrderStatus;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.domain.TruckState;
import com.hyperlogix.server.features.planification.dtos.PlanificationResultNotification;
import com.hyperlogix.server.optimizer.Optimizer;
import com.hyperlogix.server.optimizer.OptimizerContext;
import com.hyperlogix.server.optimizer.OptimizerResult;
import com.hyperlogix.server.optimizer.AntColony.AntColonyConfig;
import com.hyperlogix.server.optimizer.AntColony.AntColonyOptimizer;
import com.hyperlogix.server.services.incident.IncidentManagement;

public class PlanificationEngine implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(PlanificationEngine.class);
  private final PlanificationNotifier notifier;
  private final PLGNetwork network;
  private final LocalDateTime algorithmTime;
  private final Duration algorithmDuration;
  private volatile Thread currentThread;
  private volatile boolean isPlanning = false;
  private volatile int currentNodesProcessed = 0;
  private int currentTurn = 1;
  private List<CompletedIncident> completedIncidents;

  public PlanificationEngine(PLGNetwork network, PlanificationNotifier notifier, LocalDateTime algorithmTime,
      Duration algorithmDuration, List<CompletedIncident> completedIncidents) {
    this.notifier = notifier;
    this.network = network;
    this.algorithmTime = algorithmTime;
    this.algorithmDuration = algorithmDuration;
    this.completedIncidents = completedIncidents != null ? completedIncidents : new ArrayList<>();
  }

  @Override
  public void run() {
    currentThread = Thread.currentThread();
    isPlanning = true;

    // Count calculating orders and log details for debugging
    long calculatingOrdersCount = network.getOrders().stream()
        .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
        .count();

    currentNodesProcessed = (int) calculatingOrdersCount + network.getStations().size();

    log.info("Planification starting with {} total orders, {} calculating orders, {} stations",
        network.getOrders().size(), calculatingOrdersCount, network.getStations().size());

    // Log order details for debugging
    network.getOrders().forEach(order -> log.debug("Order {}: status={}, clientId={}, requestedGLP={}",
        order.getId(), order.getStatus(), order.getClientId(), order.getRequestedGLP()));

    try {

      updateNetworkStateFromCompletedIncidents();

      AntColonyConfig config = new AntColonyConfig(
          4,
          5,
          1.0,
          2.0,
          0.5,
          100.0,
          1.0);
      Optimizer optimizer = new AntColonyOptimizer(config);

      log.info("Running optimizer with {} trucks and {} calculating orders",
          network.getTrucks().size(), calculatingOrdersCount);

      List<Truck> availableTrucks = network.getTrucks().stream()
          .filter(truck -> truck.getStatus() == TruckState.ACTIVE)
          .collect(Collectors.toList());

      PLGNetwork filteredNetwork = createFilteredNetwork(network, availableTrucks);

      OptimizerContext ctx = new OptimizerContext(
          filteredNetwork,
          algorithmTime);

      OptimizerResult result = optimizer.run(ctx, algorithmDuration);

      Routes routes = result.getRoutes();

      log.info("Planification completed. Generated routes for {} trucks",
          routes.getStops().keySet().size());

      IncidentManagement incidentManagement = new IncidentManagement(network.getIncidents());
      String currentTurnStr = getCurrentTurn(algorithmTime);
      List<Incident> newIncidents = incidentManagement.generateIncidentsForRoutes(
          routes, currentTurnStr, algorithmTime);

      sendPlanificationResult(routes, newIncidents);

    } catch (Exception e) {
      if (Thread.currentThread().isInterrupted()) {
        return;
      }
    } finally {
      isPlanning = false;
      currentNodesProcessed = 0;
      currentThread = null;
    }
  }

  private void updateNetworkStateFromCompletedIncidents() {
    for (CompletedIncident incident : completedIncidents) {
        Truck truck = network.getTruckById(incident.getTruckId());
        if (truck != null) {
            log.info("Processing completed incident for truck {}: type {}, completed at {}",
                truck.getId(), incident.getType(), incident.getCompletionTime());
            
            // Camión disponible después del incidente
            truck.setStatus(TruckState.IDLE);
            truck.setMaintenanceStartTime(null);
            
            // Podría necesitar volver al almacén dependiendo del tipo de incidente
            if ("TYPE_2".equals(incident.getType()) || "TYPE_3".equals(incident.getType())) {
                // Estos camiones estaban en el taller, deberían estar en el almacén
                // truck.setLocation(warehouseLocation); // Si tienes la ubicación del almacén
            }
        }
    }
  }

  private String getCurrentTurn(LocalDateTime time) {
    int hour = time.getHour() % 24;
    if (hour >= 0 && hour < 8) {
      return "T1";
    } else if (hour >= 8 && hour < 16) {
      return "T2";
    } else {
      return "T3";
    }
  }

  public void stop() {
    isPlanning = false;
    Thread thread = currentThread;
    if (thread != null) {
      thread.interrupt();
    }
  }

  public PlanificationStatus getStatus() {
    return new PlanificationStatus(isPlanning, currentNodesProcessed);
  }

  public void updateNodesProcessed(int nodes) {
    this.currentNodesProcessed = nodes;
  }

  private PLGNetwork createFilteredNetwork(PLGNetwork original, List<Truck> availableTrucks) {
    // Create a copy of the network with only available trucks
    PLGNetwork filtered = original.clone();
    filtered.setTrucks(availableTrucks);
    return filtered;
  }

  private void sendPlanificationResult(Routes routes, List<Incident> incidents) {
    PlanificationResultNotification notification = new PlanificationResultNotification(routes, incidents);
    notifier.notify(notification);
  }

  private int calculateCurrentTurn(LocalDateTime algorithmTime) {
    // Calculate current turn based on algorithm time
    // This is a simplified calculation - adjust based on your turn definition
    int hour = algorithmTime.getHour();
    if (hour >= 6 && hour < 14) return 1;      // Turn 1: 6:00 - 14:00
    else if (hour >= 14 && hour < 22) return 2; // Turn 2: 14:00 - 22:00
    else return 3;                             // Turn 3: 22:00 - 6:00
  }

}
