package com.hyperlogix.server.services.simulation;

import com.hyperlogix.server.domain.*;

import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.hyperlogix.server.features.planification.dtos.PlanificationRequestEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SimulationEngine implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);

  private final ApplicationEventPublisher eventPublisher;
  private final String sessionId;
  private final SimulationConfig simulationConfig;
  private final SimulationNotifier simulationNotifier;
  private final List<Order> orderRepository;

  @Setter
  private PLGNetwork plgNetwork;
  private Routes activeRoutes;
  private final Object routesLock = new Object();
  private LocalDateTime nextPlanningTime;
  private LocalDateTime simulatedTime;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicBoolean paused = new AtomicBoolean(false);
  private final Lock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();
  private final Map<String, Integer> truckCurrentStopIndex = new ConcurrentHashMap<>();

  public SimulationEngine(String sessionId,
      SimulationConfig simulationConfig,
      SimulationNotifier simulationNotifier,
      List<Order> orderRepository, ApplicationEventPublisher eventPublisher) {
    this.sessionId = sessionId;
    this.simulationConfig = simulationConfig;
    this.simulationNotifier = simulationNotifier;
    this.orderRepository = orderRepository;
    this.eventPublisher = eventPublisher;
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
        getOrderBatch(simulatedTime);
        requestPlanification();
        nextPlanningTime = nextPlanningTime
            .plus(simulationConfig.getConsumptionInterval());
        log.info("Next planning time: {}", nextPlanningTime);
      }
      updateSystemState(timeStep);
      simulationNotifier.notifySnapshot(new SimulationSnapshot(LocalDateTime.now(), simulatedTime, plgNetwork));
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
    if (activeRoutes.getStops().isEmpty() || activeRoutes.getPaths().isEmpty()) {
      log.warn("No active routes or stops available, skipping update");
      return;
    }

    synchronized (routesLock) {
      for (Truck truck : plgNetwork.getTrucks()) {
        if (truck.getStatus() == TruckState.MAINTENANCE || truck.getStatus() == TruckState.BROKEN_DOWN) {
          continue;
        }

        List<Stop> stops = activeRoutes.getStops().getOrDefault(truck.getId(), List.of());
        if (stops.isEmpty()) {
          continue;
        }

        List<Path> paths = activeRoutes.getPaths().getOrDefault(truck.getId(), List.of());
        int currentStopIndex = truckCurrentStopIndex.getOrDefault(truck.getId(), 0);

        if (currentStopIndex >= stops.size()) {
          log.debug("Truck {} has completed all stops", truck.getId());
          continue;
        }

        Stop currentStop = stops.get(currentStopIndex);

        // Check if truck has arrived at current stop
        if (simulatedTime.isAfter(currentStop.getArrivalTime()) ||
            simulatedTime.equals(currentStop.getArrivalTime())) {

          // Handle arrival at stop
          handleStopArrival(truck, currentStop);

          // Move to next stop
          truckCurrentStopIndex.put(truck.getId(), currentStopIndex + 1);
          currentStopIndex++;

          if (currentStopIndex >= stops.size()) {
            log.info("Truck {} completed all deliveries", truck.getId());
            continue;
          }
        }

        // Update truck location during travel
        if (currentStopIndex < stops.size()) {
          Stop nextStop = stops.get(currentStopIndex);
          updateTruckLocationDuringTravel(truck, currentStop, nextStop, paths, currentStopIndex, timeStep);
        }
      }
    }
  }

  private void handleStopArrival(Truck truck, Stop stop) {
    log.info("Truck {} arrived at stop {} at {}", truck.getId(), stop.getNode().getId(), simulatedTime);

    if (stop.getNode().getType() == NodeType.STATION) {
      handleStationArrival(truck, stop);
    } else if (stop.getNode().getType() == NodeType.DELIVERY) {
      handleDeliveryArrival(truck, stop);
    }

    // Update truck location
    truck.setLocation(stop.getNode().getLocation());
  }

  private void handleStationArrival(Truck truck, Stop stop) {
    Station station = plgNetwork.getStations().stream()
        .filter(s -> s.getId().equals(stop.getNode().getId()))
        .findFirst().orElse(null);

    if (station == null) {
      log.error("Station not found: {}", stop.getNode().getId());
      return;
    }

    // Refuel truck
    truck.setCurrentFuel(truck.getFuelCapacity());

    // Refill GLP capacity
    int glpToFull = truck.getMaxCapacity() - truck.getCurrentCapacity();
    int availableCapacity = station.getAvailableCapacity(simulatedTime);
    int glpToRefill = Math.min(glpToFull, availableCapacity);

    if (glpToRefill > 0) {
      station.reserveCapacity(simulatedTime, glpToRefill);
      truck.setCurrentCapacity(truck.getCurrentCapacity() + glpToRefill);
      log.info("Truck {} refilled {} GLP at station {}", truck.getId(), glpToRefill, station.getName());
    } else {
      log.warn("No GLP available at station {} for truck {}", station.getName(), truck.getId());
    }
  }

  private void handleDeliveryArrival(Truck truck, Stop stop) {
    Order order = orderRepository.stream()
        .filter(o -> o.getId().equals(stop.getNode().getId()))
        .findFirst().orElse(null);

    if (order == null) {
      log.error("Order not found: {}", stop.getNode().getId());
      return;
    }

    // Calculate delivery amount
    int remainingGLP = order.getRequestedGLP() - order.getDeliveredGLP();
    int glpToDeliver = Math.min(truck.getCurrentCapacity(), remainingGLP);

    if (glpToDeliver > 0) {
      // Update order
      order.setDeliveredGLP(order.getDeliveredGLP() + glpToDeliver);

      // Update truck capacity
      truck.setCurrentCapacity(truck.getCurrentCapacity() - glpToDeliver);

      log.info("Truck {} delivered {} GLP to order {} (total delivered: {}/{})",
          truck.getId(), glpToDeliver, order.getId(),
          order.getDeliveredGLP(), order.getRequestedGLP());

      // Check if order is completed
      if (order.getDeliveredGLP() >= order.getRequestedGLP()) {
        order.setStatus(OrderStatus.COMPLETED);
        log.info("Order {} completed", order.getId());
      }
    } else {
      log.warn("Truck {} has no capacity to deliver to order {}", truck.getId(), order.getId());
    }
  }

  private void updateTruckLocationDuringTravel(Truck truck, Stop currentStop, Stop nextStop,
      List<Path> paths, int currentStopIndex, Duration timeStep) {
    if (currentStopIndex >= paths.size()) {
      return;
    }

    Path path = paths.get(currentStopIndex);

    // Calculate travel progress
    LocalDateTime departureTime = currentStop.getArrivalTime();
    LocalDateTime arrivalTime = nextStop.getArrivalTime();

    if (simulatedTime.isBefore(departureTime)) {
      // Haven't departed yet
      return;
    }

    Duration totalTravelTime = Duration.between(departureTime, arrivalTime);
    Duration elapsedTime = Duration.between(departureTime, simulatedTime);

    double progress = Math.max(0.0, Math.min(1.0,
        elapsedTime.toMillis() / (double) totalTravelTime.toMillis()));

    // Update truck location along path
    int pathPointIndex = Math.min((int) (progress * (path.points().size() - 1)),
        path.points().size() - 1);
    truck.setLocation(path.points().get(pathPointIndex));

    // Consume fuel during travel (proportional to progress since last update)
    double fuelConsumptionRate = truck.getFuelConsumption(path.length()) / totalTravelTime.toMillis();
    double fuelConsumed = fuelConsumptionRate * timeStep.toMillis();
    truck.setCurrentFuel(Math.max(0, truck.getCurrentFuel() - fuelConsumed));

    if (truck.getCurrentFuel() <= 0) {
      log.warn("Truck {} ran out of fuel!", truck.getId());
      truck.setStatus(TruckState.BROKEN_DOWN);
    }
  }

  private void getOrderBatch(LocalDateTime currenDateTime) {
    List<Order> newOrders = orderRepository.stream()
        .filter(order -> order.getDate().isBefore(currenDateTime) && order.getStatus() == OrderStatus.PENDING).toList();

    if (!newOrders.isEmpty()) {
      // Set new orders to CALCULATING
      newOrders.forEach(order -> {
        order.setStatus(OrderStatus.CALCULATING);
        log.info("New order {} added to batch for calculation", order.getId());
      });

      // Also recalculate existing IN_PROGRESS orders when new orders arrive
      List<Order> inProgressOrders = orderRepository.stream()
          .filter(order -> order.getStatus() == OrderStatus.IN_PROGRESS)
          .toList();

      inProgressOrders.forEach(order -> {
        order.setStatus(OrderStatus.CALCULATING);
        log.info("Order {} set back to CALCULATING due to new batch", order.getId());
      });
    }
  }

  private void requestPlanification() {
    // Only request planification if there are orders being calculated
    boolean hasCalculatingOrders = orderRepository.stream()
        .anyMatch(order -> order.getStatus() == OrderStatus.CALCULATING);

    if (hasCalculatingOrders) {
      log.info("Requesting planification from {}", sessionId);
      eventPublisher.publishEvent(new PlanificationRequestEvent(sessionId, plgNetwork, simulatedTime));
    } else {
      log.debug("No orders in CALCULATING status, skipping planification request");
    }
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
    synchronized (routesLock) {
      this.activeRoutes = routes;
      // Reset stop indices when new routes are received
      truckCurrentStopIndex.clear();
    }
    List<Order> calculatingOrders = orderRepository.stream()
        .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
        .toList();

    calculatingOrders.forEach(order -> {
      order.setStatus(OrderStatus.IN_PROGRESS);
    });

    log.info("Received updated routes from Planificator");
  }

  public SimulationStatus getStatus() {
    return new SimulationStatus(
        running.get(),
        paused.get(),
        simulationConfig.timeAcceleration());
  }
}
