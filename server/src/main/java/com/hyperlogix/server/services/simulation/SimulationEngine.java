package com.hyperlogix.server.services.simulation;

import com.hyperlogix.server.domain.*;

import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimulationEngine implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);

  private final String sessionId;
  private final SimulationConfig simulationConfig;
  private final SimulationNotifier simulationNotifier;
  private final List<Order> orderRepository;
  @Setter
  private PLGNetwork plgNetwork;
  private Routes activeRoutes;
  private LocalDateTime nextPlanningTime;
  private LocalDateTime simulatedTime;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicBoolean paused = new AtomicBoolean(false);
  private final Lock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();

  public SimulationEngine(String sessionId,
      SimulationConfig simulationConfig,
      SimulationNotifier simulationNotifier,
      List<Order> orderRepository) {
    this.sessionId = sessionId;
    this.simulationConfig = simulationConfig;
    this.simulationNotifier = simulationNotifier;
    this.orderRepository = orderRepository;
  }

  @Override
  public void run() {
    running.set(true);
    log.info("=====Simulation started=====");
    log.info("Config K={}, Sa={}, Sc={}, Ta={}",
        simulationConfig.timeAcceleration(),
        simulationConfig.algorithmInterval(),
        simulationConfig.getConsumptionInterval(),
        simulationConfig.algorithmTime());

    simulatedTime = orderRepository.getFirst().getDate().plus(Duration.ofNanos(1));

    Duration timeStep = simulationConfig.simulationResolution().multipliedBy(simulationConfig.timeAcceleration());
    nextPlanningTime = simulatedTime;

    while (running.get()) {
      waitIfPaused();

      if (!running.get())
        break;

      simulatedTime = simulatedTime.plus(timeStep);
      if (simulatedTime.isAfter(nextPlanningTime)) {
        log.info("Simulated time: {}", simulatedTime);
        List<Order> newOrders = getOrderBatch(simulatedTime);
        log.info("New orders: {}", newOrders.size());
        requestPlanification();
        nextPlanningTime = nextPlanningTime
            .plus(simulationConfig.getConsumptionInterval());
        log.info("Next planning time: {}", nextPlanningTime);
      }
      updateSystemState(timeStep);
      simulationNotifier.notifySnapshot(new SimulationSnapshot(simulatedTime, plgNetwork));
      sleep(simulationConfig.simulationResolution());
    }
  }

  public void stop() {
    log.info("Stopping simulation...");
    running.set(false);
    lock.lock();
    try {
      condition.signalAll();
    } finally {
      lock.unlock();
    }
  }

  public void handleCommand(String command) {
    switch (command.toUpperCase()) {
      case "PAUSE" -> {
        paused.set(true);
        log.info("Simulation paused");
      }
      case "RESUME" -> {
        paused.set(false);
        lock.lock();
        try {
          condition.signalAll();
        } finally {
          lock.unlock();
        }
        log.info("Simulation resumed");
      }
      case "STOP" -> {
        running.set(false);
        stop();
      }
      default -> log.warn("Unknown command: {}", command);
    }
  }

  private void updateSystemState(Duration timeStep) {
    log.trace("Updating system state: {}", timeStep);
    if (activeRoutes == null) {
      log.warn("Active routes are not set, skipping update");
      return;
    }
    for (Truck truck : plgNetwork.getTrucks()) {
      List<Stop> stops = activeRoutes.getStops().getOrDefault(truck.getId(), List.of());
      if (stops.isEmpty())
        continue;
      List<Path> paths = activeRoutes.getPaths().getOrDefault(truck.getId(), List.of());

      Stop nextStop = stops.stream()
          .filter(stop -> stop.getArrivalTime()
              .isBefore(simulatedTime.minus(timeStep.multipliedBy(simulationConfig.timeAcceleration()))))
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

  private List<Order> getOrderBatch(LocalDateTime currenDateTime) {
    if (orderRepository.isEmpty()) {
      return List.of();
    }
    List<Order> newOrders = orderRepository.stream()
        .filter(order -> order.getDate().isBefore(currenDateTime) && order.getStatus() == OrderStatus.PENDING).toList();
    return newOrders;
  }

  private void requestPlanification() {
    log.info("Requesting planification from {}", sessionId);

  }

  private void sleep(Duration duration) {
    lock.lock();
    try {
      if (!running.get())
        return;
      condition.await(duration.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      running.set(false);
      log.error("Simulation interrupted", e);
    } finally {
      lock.unlock();
    }
  }

  private void waitIfPaused() {
    lock.lock();
    try {
      while (paused.get() && running.get()) {
        condition.await();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      running.set(false);
      log.warn("Simulation paused wait interrupted");
    } finally {
      lock.unlock();
    }
  }

  public void onPlanificationResult(Routes routes) {
    synchronized (activeRoutes) {
      this.activeRoutes = routes;
    }
    log.info("Received updated routes from Planificator");
  }

  public SimulationStatus getStatus() {
    return new SimulationStatus(
        running.get(),
        paused.get(),
        simulationConfig.timeAcceleration());
  }
}
