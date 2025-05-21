package com.hyperlogix.server.simulation;

import com.hyperlogix.server.domain.*;
import com.hyperlogix.server.optimizer.Optimizer;
import com.hyperlogix.server.optimizer.OptimizerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimulationEngine {
  private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);
  private final PLGNetwork plgNetwork;
  private final SimulationConfig config;
  private final SimulationNotifier notifier;
  private final Optimizer optimizer;
  private Routes activeRoutes;
  LocalDateTime simulatedTime;
  private final Lock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();

  public SimulationEngine(PLGNetwork plgNetwork, Optimizer optimizer, SimulationConfig config,
      SimulationNotifier notifier) {
    this.plgNetwork = plgNetwork;
    this.optimizer = optimizer;
    this.config = config;
    this.notifier = notifier;
    this.activeRoutes = new Routes(new HashMap<>(), null, 0);
  }

  public void run() {
    log.info("=====Simulation started=====");
    log.info("Config K={}, Sa={}, Sc={}, Ta={}, Duration={}",
        config.timeAcceleration(),
        config.algorithmInterval(),
        config.getConsumptionInterval(),
        config.algorithmTime(),
        config.simulationDuration());
    Duration timeStep = Duration.ofMillis(100L * config.timeAcceleration());
    simulatedTime = config.simulationStartTime();
    LocalDateTime simulationEndTime = simulatedTime.plus(config.simulationDuration()
        .multipliedBy(config.timeAcceleration()));
    LocalDateTime nextPlanningTime = simulatedTime;
    while (simulatedTime.isBefore(simulationEndTime)) {
      if (!simulatedTime.isBefore(nextPlanningTime)) {
        log.info("Simulated time: {}", simulatedTime);
        LocalDateTime finalSimulatedTime = simulatedTime;
        CompletableFuture.supplyAsync(() -> optimizer.run(
            new OptimizerContext(plgNetwork, finalSimulatedTime), config.algorithmTime())).thenAccept(
                result -> {
                  synchronized (activeRoutes) {
                    activeRoutes = result.getRoutes();
                  }
                  log.info(">>> Best Routes: {}", result.getRoutes());
                });
        nextPlanningTime = nextPlanningTime.plus(config.algorithmInterval());
        log.info("Next planning time: {}", nextPlanningTime);
      }
      updateSystemState(timeStep);
      notifier.notifySnapshot(new SimulationSnapshot(simulatedTime, plgNetwork.clone()));
      simulatedTime = simulatedTime.plus(timeStep);
      try {
        lock.lock();
        long remainingNanos = config.simulationResolution().toNanos();
        while (remainingNanos > 0) {
          remainingNanos = condition.awaitNanos(remainingNanos);
        }
      } catch (InterruptedException e) {
        log.error("Simulation interrupted", e);
        Thread.currentThread().interrupt();
      } finally {
        lock.unlock();
      }
    }
  }

  private void updateSystemState(Duration timeStep) {
    log.trace("Updating system state: {}", timeStep);

    for (Truck truck : plgNetwork.getTrucks()) {
      List<Stop> stops = activeRoutes.getStops().getOrDefault(truck.getId(), List.of());
      if (stops.isEmpty())
        continue;
      List<Path> paths = activeRoutes.getPaths().getOrDefault(truck.getId(), List.of());

      Stop nextStop = stops.stream()
          .filter(stop -> stop.getArrivalTime()
              .isBefore(simulatedTime.minus(timeStep.multipliedBy(config.timeAcceleration()))))
          .findFirst()
          .orElse(null);

      if (nextStop != null) {
        int index = stops.indexOf(nextStop);
        Path path = paths.get(index);

        double progress = 1 - Duration.between(simulatedTime, nextStop.getArrivalTime())
            .toMillis() / (double) truck.getTimeToDestination(path.length()).toMillis();

        truck.setLocation(path.points().get((int) (progress * path.length())));
      }
    }
  }
}
