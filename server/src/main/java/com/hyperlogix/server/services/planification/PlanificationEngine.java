package com.hyperlogix.server.services.planification;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hyperlogix.server.domain.OrderStatus;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.optimizer.Optimizer;
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
  private volatile Thread currentThread;
  private volatile boolean isPlanning = false;
  private volatile int currentNodesProcessed = 0;

  public PlanificationEngine(PLGNetwork network, PlanificationNotifier notifier, LocalDateTime algorithmTime,
      Duration algorithmDuration) {
    this.notifier = notifier;
    this.network = network;
    this.algorithmTime = algorithmTime;
    this.algorithmDuration = algorithmDuration;
  }

  @Override
  public void run() {
    log.info("Starting planification for network task");
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
      AntColonyConfig config = new AntColonyConfig(
          4,
          10,
          1.0,
          2.0,
          0.5,
          100.0,
          1.0);
      Optimizer optimizer = new AntColonyOptimizer(config);

      OptimizerContext ctx = new OptimizerContext(
          network,
          algorithmTime);

      log.info("Running optimizer with {} trucks and {} calculating orders",
          network.getTrucks().size(), calculatingOrdersCount);

      OptimizerResult result = optimizer.run(ctx, algorithmDuration);
      Routes routes = result.getRoutes();

      log.info("Planification completed. Generated routes for {} trucks",
          routes.getStops().keySet().size());

      sendPlanificationResult(routes);
    } catch (Exception e) {
      if (Thread.currentThread().isInterrupted()) {
        log.info("Planification execution was interrupted");
        return;
      }
      log.error("Error during planification", e);
    } finally {
      isPlanning = false;
      currentNodesProcessed = 0;
      currentThread = null;
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
