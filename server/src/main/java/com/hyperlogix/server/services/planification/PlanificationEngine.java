package com.hyperlogix.server.services.planification;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import com.hyperlogix.server.domain.OrderStatus;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Incident;

import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.optimizer.OptimizerContext;
import com.hyperlogix.server.optimizer.OptimizerResult;
import com.hyperlogix.server.optimizer.AntColony.AntColonyConfig;
import com.hyperlogix.server.optimizer.AntColony.AntColonyOptimizer;

public class PlanificationEngine implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(PlanificationEngine.class);
  private final PlanificationNotifier notifier;
  private final PLGNetwork network;
  private final LocalDateTime algorithmTime;
  private final Duration algorithmDuration;
  private final ApplicationEventPublisher eventPublisher;
  private final String sessionId;
  private final List<Incident> incidents;
  private final Runnable onComplete;
  private volatile Thread currentThread;
  private volatile boolean isPlanning = false;
  private volatile int currentNodesProcessed = 0;

  public PlanificationEngine(PLGNetwork network, PlanificationNotifier notifier, LocalDateTime algorithmTime,
      Duration algorithmDuration, List<Incident> incidents, ApplicationEventPublisher eventPublisher, String sessionId, Runnable onComplete) {
    this.notifier = notifier;
    this.network = network;
    this.algorithmTime = algorithmTime;
    this.algorithmDuration = algorithmDuration;
    this.eventPublisher = eventPublisher;
    this.sessionId = sessionId;
    this.incidents = incidents != null ? incidents : List.of();
    this.onComplete = onComplete;
  }

  // Constructor sin eventos para compatibilidad hacia atrás
  public PlanificationEngine(PLGNetwork network, PlanificationNotifier notifier, LocalDateTime algorithmTime,
      Duration algorithmDuration, List<Incident> incidents) {
    this(network, notifier, algorithmTime, algorithmDuration, incidents, null, null, null);
  }

  @Override
  public void run() {
    currentThread = Thread.currentThread();
    isPlanning = true;

    // Count calculating orders and log details for debugging
    long calculatingOrdersCount = network.getOrders().stream()
        .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
        .count();

    currentNodesProcessed = (int) calculatingOrdersCount + network.getStations().size() + (incidents != null ? incidents.size() : 0);

    log.info("Planification starting with {} total orders, {} calculating orders, {} stations and {} incidents",
        network.getOrders().size(), calculatingOrdersCount, network.getStations().size(), (incidents != null ? incidents.size() : 0));

    // Debug crítico para el día 3 de enero 2025 a las 4:30+ AM
    if (algorithmTime.getYear() == 2025 && algorithmTime.getMonthValue() == 1 && 
        algorithmTime.getDayOfMonth() == 3 && algorithmTime.getHour() >= 4 && algorithmTime.getMinute() >= 30) {
      
      log.error("=== CRITICAL PLANIFICATION DEBUG === Time: {}", algorithmTime);
      log.error("=== NETWORK VALIDATION === Orders: {}, CalculatingOrders: {}, Trucks: {}, Stations: {}", 
               network.getOrders().size(), calculatingOrdersCount, 
               network.getTrucks().size(), network.getStations().size());
      
      // Validar integridad de la red
      if (network.getOrders().isEmpty()) {
        log.error("=== CRITICAL ERROR === Empty orders list in planification network!");
      }
      if (network.getTrucks().stream().noneMatch(t -> t.getStatus() == com.hyperlogix.server.domain.TruckState.IDLE || t.getStatus() == com.hyperlogix.server.domain.TruckState.ACTIVE)) {
        log.error("=== CRITICAL ERROR === No available trucks for planification!");
      }
      
      // Log detalles de órdenes calculando
      network.getOrders().stream()
          .filter(order -> order.getStatus() == com.hyperlogix.server.domain.OrderStatus.CALCULATING)
          .forEach(order -> log.error("  Calculating order: {} at ({},{}) requested: {}m3", 
                                    order.getId(), order.getLocation().x(), order.getLocation().y(), 
                                    order.getRequestedGLP()));
    }

    // Log order details for debugging
    network.getOrders().forEach(order -> log.debug("Order {}: status={}, clientId={}, requestedGLP={}",
        order.getId(), order.getStatus(), order.getClientId(), order.getRequestedGLP()));

    try {
      AntColonyConfig config = new AntColonyConfig(
          4,
          5,
          1.0,
          2.0,
          0.5,
          100.0,
          1.0);
      AntColonyOptimizer optimizer = new AntColonyOptimizer(config);

      // Configurar el event publisher y session ID si están disponibles
      if (eventPublisher != null && sessionId != null) {
        optimizer.setEventPublisher(eventPublisher);
        optimizer.setSessionId(sessionId);
      }

      OptimizerContext ctx = new OptimizerContext(
          network,
          algorithmTime,
          incidents);

      log.info("Running optimizer with {} trucks and {} calculating orders",
          network.getTrucks().size(), calculatingOrdersCount);

      OptimizerResult result = optimizer.run(ctx, algorithmDuration);

      Routes routes = result.getRoutes();

      log.info("Planification completed. Generated routes for {} trucks",
          routes.getStops().keySet().size());

      sendPlanificationResult(routes);
    } catch (Exception e) {
      if (Thread.currentThread().isInterrupted()) {
        return;
      }
    } finally {
      isPlanning = false;
      currentNodesProcessed = 0;
      currentThread = null;
      if (onComplete != null) {
        onComplete.run();
      }
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

  private void sendPlanificationResult(Routes routes) {
    notifier.notify(routes);
  }

}
