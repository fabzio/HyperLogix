package com.hyperlogix.server.services.planification;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private volatile Thread currentThread;

  public PlanificationEngine(PLGNetwork network, PlanificationNotifier notifier) {
    this.notifier = notifier;
    this.network = network;
  }

  @Override
  public void run() {
    currentThread = Thread.currentThread();

    try {
      AntColonyConfig config = new AntColonyConfig(
          4,
          10,
          1.0,
          2.0,
          0.5, 
          100.0,
          1.0
      );
      Optimizer optimizer = new AntColonyOptimizer(config);

      OptimizerContext ctx = new OptimizerContext(
          network,
          LocalDateTime.now());

      OptimizerResult result = optimizer.run(ctx, Duration.ofSeconds(5));
      Routes routes = result.getRoutes();
      sendPlanificationResult(routes);
    } catch (Exception e) {
      if (Thread.currentThread().isInterrupted()) {
        log.info("Planification execution was interrupted");
        return;
      }
      log.error("Error during planification", e);
    } finally {
      currentThread = null;
    }
  }

  public void stop() {
    Thread thread = currentThread;
    if (thread != null) {
      thread.interrupt();
    }
  }

  private void sendPlanificationResult(Routes routes) {
    notifier.notify(routes);
  }

}
