package com.hyperlogix.server.services.simulation;

import com.hyperlogix.server.config.Constants;
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

  // Metrics tracking fields
  private final Map<String, Double> truckFuelConsumed = new ConcurrentHashMap<>();
  private final Map<String, Double> truckDistanceTraveled = new ConcurrentHashMap<>();
  private final Map<String, Integer> truckDeliveryCount = new ConcurrentHashMap<>();
  private final Map<String, Duration> customerDeliveryTimes = new ConcurrentHashMap<>();
  private final Map<String, LocalDateTime> orderStartTimes = new ConcurrentHashMap<>();
  private int totalPlanificationRequests = 0;
  private Duration totalPlanificationTime = Duration.ZERO;
  private LocalDateTime lastPlanificationStart;

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

      // Calculate metrics and notify with snapshot
      SimulationMetrics metrics = calculateMetrics();
      simulationNotifier
          .notifySnapshot(
              new SimulationSnapshot(LocalDateTime.now(), simulatedTime, plgNetwork, activeRoutes, metrics));
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
          updateTruckLocationDuringTravel(truck, stops, paths, currentStopIndex - 1);
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

      // Track delivery metrics
      truckDeliveryCount.merge(truck.getId(), 1, Integer::sum);

      // Calculate delivery time if order just started
      if (order.getDeliveredGLP() == glpToDeliver) { // First delivery for this order
        orderStartTimes.put(order.getId(), simulatedTime);
      }

      log.info("Truck {} delivered {} GLP to order {} (total delivered: {}/{})",
          truck.getId(), glpToDeliver, order.getId(),
          order.getDeliveredGLP(), order.getRequestedGLP());

      // Check if order is completed
      if (order.getDeliveredGLP() >= order.getRequestedGLP()) {
        order.setStatus(OrderStatus.COMPLETED);

        // Calculate delivery time
        LocalDateTime startTime = orderStartTimes.get(order.getId());
        if (startTime != null) {
          Duration deliveryTime = Duration.between(startTime, simulatedTime);
          customerDeliveryTimes.put(order.getClientId(), deliveryTime);
        }

        log.info("Order {} completed", order.getId());
      }
    } else {
      log.warn("Truck {} has no capacity to deliver to order {}", truck.getId(), order.getId());
    }
  }

  private void updateTruckLocationDuringTravel(Truck truck, List<Stop> stops, List<Path> paths, int currentStopIndex) {
    if (currentStopIndex >= stops.size() - 1 || currentStopIndex >= paths.size()) {
      return; // No more paths to travel
    }

    Stop currentStop = stops.get(currentStopIndex);
    Path currentPath = paths.get(currentStopIndex);

    // Calculate time elapsed since leaving current stop
    Duration timeElapsed = Duration.between(currentStop.getArrivalTime(), simulatedTime);
    double hoursElapsed = timeElapsed.toSeconds() / 3600.0;

    // Calculate distance traveled based on truck speed
    double distanceTraveled = hoursElapsed * Constants.TRUCK_SPEED;

    // Calculate total path distance in km
    double totalPathDistance = currentPath.length();

    // Calculate progress along the path (0.0 to 1.0)
    double progress = Math.min(distanceTraveled / totalPathDistance, 1.0);

    // Calculate distance traveled in this simulation step only
    Duration stepDuration = simulationConfig.simulationResolution().multipliedBy(simulationConfig.timeAcceleration());
    double stepHours = stepDuration.toSeconds() / 3600.0;
    double stepDistance = stepHours * Constants.TRUCK_SPEED;

    // Track metrics
    truckDistanceTraveled.merge(truck.getId(), stepDistance, Double::sum);

    // Calculate fuel consumption for the step distance only
    double fuelConsumed = truck.getFuelConsumption(stepDistance);

    // Track fuel consumption
    truckFuelConsumed.merge(truck.getId(), fuelConsumed, Double::sum);

    // Update truck fuel (ensure it doesn't go below 0)
    double newFuelLevel = Math.max(0, truck.getCurrentFuel() - fuelConsumed);
    truck.setCurrentFuel(newFuelLevel);

    // Interpolate position along the path
    Point interpolatedPosition = interpolateAlongPath(currentPath.points(), progress);

    // Update truck location
    truck.setLocation(interpolatedPosition);

    log.trace("Truck {} at position ({}, {}) - progress: {:.2f}%, fuel: {:.2f}gal",
        truck.getId(), interpolatedPosition.x(), interpolatedPosition.y(), progress * 100, newFuelLevel);
  }

  private Point interpolateAlongPath(List<Point> pathPoints, double progress) {
    if (pathPoints.isEmpty()) {
      return new Point(0.0, 0.0);
    }

    if (pathPoints.size() == 1) {
      return pathPoints.get(0);
    }

    // Calculate total path length in segments
    double totalLength = 0;
    for (int i = 0; i < pathPoints.size() - 1; i++) {
      Point p1 = pathPoints.get(i);
      Point p2 = pathPoints.get(i + 1);
      totalLength += Math.sqrt(Math.pow(p2.x() - p1.x(), 2) + Math.pow(p2.y() - p1.y(), 2));
    }

    // Find target distance along path
    double targetDistance = progress * totalLength;
    double currentDistance = 0;

    // Find which segment we're in
    for (int i = 0; i < pathPoints.size() - 1; i++) {
      Point p1 = pathPoints.get(i);
      Point p2 = pathPoints.get(i + 1);
      double segmentLength = Math.sqrt(Math.pow(p2.x() - p1.x(), 2) + Math.pow(p2.y() - p1.y(), 2));

      if (currentDistance + segmentLength >= targetDistance) {
        // We're in this segment, interpolate within it
        double segmentProgress = (targetDistance - currentDistance) / segmentLength;

        double interpolatedX = p1.x() + (p2.x() - p1.x()) * segmentProgress;
        double interpolatedY = p1.y() + (p2.y() - p1.y()) * segmentProgress;

        return new Point(interpolatedX, interpolatedY);
      }

      currentDistance += segmentLength;
    }

    // If we've gone past the end, return the last point
    return pathPoints.get(pathPoints.size() - 1);
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
      lastPlanificationStart = LocalDateTime.now();
      totalPlanificationRequests++;
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

    // Track planification time
    if (lastPlanificationStart != null) {
      Duration planificationDuration = Duration.between(lastPlanificationStart, LocalDateTime.now());
      totalPlanificationTime = totalPlanificationTime.plus(planificationDuration);
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

  private SimulationMetrics calculateMetrics() {
    // Fleet utilization
    int totalTrucks = plgNetwork.getTrucks().size();
    int activeTrucks = (int) plgNetwork.getTrucks().stream()
        .filter(t -> t.getStatus() != TruckState.MAINTENANCE && t.getStatus() != TruckState.BROKEN_DOWN)
        .count();
    double fleetUtilization = totalTrucks > 0 ? (double) activeTrucks / totalTrucks * 100 : 0;

    // Fuel efficiency
    double totalFuelConsumed = truckFuelConsumed.values().stream().mapToDouble(Double::doubleValue).sum();
    double totalDistance = truckDistanceTraveled.values().stream().mapToDouble(Double::doubleValue).sum();
    double avgFuelConsumption = totalDistance > 0 ? totalFuelConsumed / totalDistance : 0;

    // Delivery performance
    int totalOrders = orderRepository.size();
    int completedOrders = (int) orderRepository.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count();
    double completionPercentage = totalOrders > 0 ? (double) completedOrders / totalOrders * 100 : 0;
    double avgDeliveryTime = customerDeliveryTimes.values().stream()
        .mapToDouble(d -> d.toMinutes())
        .average().orElse(0);

    // Capacity utilization
    double avgCapacityUtilization = plgNetwork.getTrucks().stream()
        .mapToDouble(t -> (double) t.getCurrentCapacity() / t.getMaxCapacity() * 100)
        .average().orElse(0);

    // Planning efficiency
    double avgPlanificationTimeSeconds = totalPlanificationRequests > 0
        ? totalPlanificationTime.toSeconds() / (double) totalPlanificationRequests
        : 0;

    // GLP delivery efficiency
    int totalGLPRequested = orderRepository.stream().mapToInt(Order::getRequestedGLP).sum();
    int totalGLPDelivered = orderRepository.stream().mapToInt(Order::getDeliveredGLP).sum();
    double deliveryEfficiency = totalGLPRequested > 0 ? (double) totalGLPDelivered / totalGLPRequested * 100 : 0;

    return new SimulationMetrics(
        fleetUtilization,
        avgFuelConsumption,
        completionPercentage,
        avgDeliveryTime,
        avgCapacityUtilization,
        avgPlanificationTimeSeconds,
        totalDistance,
        deliveryEfficiency);
  }
}
