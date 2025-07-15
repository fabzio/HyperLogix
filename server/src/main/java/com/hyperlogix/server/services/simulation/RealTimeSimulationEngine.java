package com.hyperlogix.server.services.simulation;

import com.hyperlogix.server.config.Constants;
import com.hyperlogix.server.domain.*;
import com.hyperlogix.server.features.operation.repository.RealTimeOrderRepository;
import com.hyperlogix.server.features.planification.dtos.PlanificationRequestEvent;
import com.hyperlogix.server.services.planification.PlanificationService;
import com.hyperlogix.server.services.planification.PlanificationStatus;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class RealTimeSimulationEngine implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(RealTimeSimulationEngine.class);

  private final ApplicationEventPublisher eventPublisher;
  private final PlanificationService planificationService;
  private final String sessionId;
  private final SimulationConfig simulationConfig;
  private final SimulationNotifier simulationNotifier;
  private final RealTimeOrderRepository realTimeOrderRepository;

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

  // Metrics tracking fields with periodic cleanup
  private final Map<String, Double> truckFuelConsumed = new ConcurrentHashMap<>();
  private final Map<String, Double> truckDistanceTraveled = new ConcurrentHashMap<>();
  private final Map<String, Integer> truckDeliveryCount = new ConcurrentHashMap<>();
  private final Map<String, Duration> customerDeliveryTimes = new ConcurrentHashMap<>();
  private final Map<String, LocalDateTime> orderStartTimes = new ConcurrentHashMap<>();
  private int totalPlanificationRequests = 0;
  private Duration totalPlanificationTime = Duration.ZERO;
  private LocalDateTime lastPlanificationStart;

  // Order arrival rate tracking with automatic cleanup
  private final Map<LocalDateTime, Integer> orderArrivalHistory = new ConcurrentHashMap<>();
  private LocalDateTime lastOrderRateCheck = null;
  private LocalDateTime lastMetricsCleanup = null;
  private static final Duration ORDER_RATE_CHECK_WINDOW = Duration.ofMinutes(10);
  private static final Duration METRICS_CLEANUP_INTERVAL = Duration.ofMinutes(30);

  // Order change tracking to prevent unnecessary replanification
  private String lastOrdersSnapshot = "";
  private boolean forceReplanification = false;

  public RealTimeSimulationEngine(String sessionId,
      SimulationConfig simulationConfig,
      SimulationNotifier simulationNotifier,
      RealTimeOrderRepository realTimeOrderRepository,
      ApplicationEventPublisher eventPublisher,
      PlanificationService planificationService) {
    this.sessionId = sessionId;
    this.simulationConfig = simulationConfig;
    this.simulationNotifier = simulationNotifier;
    this.realTimeOrderRepository = realTimeOrderRepository;
    this.eventPublisher = eventPublisher;
    this.planificationService = planificationService;
  }

  @Override
  public void run() {
    running.set(true);
    log.info("Config K={}, Sa={}, Sc={}, Ta={}",
        simulationConfig.getTimeAcceleration(),
        simulationConfig.getCurrentAlgorithmInterval(),
        simulationConfig.getConsumptionInterval(),
        simulationConfig.getAlgorithmTime());

    // Start with current time for real-time simulation
    simulatedTime = LocalDateTime.now();
    nextPlanningTime = simulatedTime;
    lastOrderRateCheck = simulatedTime;
    lastMetricsCleanup = simulatedTime;

    while (running.get()) {
      waitIfPaused();

      if (!running.get())
        break;

      Duration timeStep = simulationConfig.getSimulationResolution()
          .multipliedBy((long) simulationConfig.getTimeAcceleration());

      simulatedTime = simulatedTime.plus(timeStep);

      // Check and adjust algorithm interval based on order arrival rate
      if (simulatedTime.isAfter(lastOrderRateCheck.plus(ORDER_RATE_CHECK_WINDOW))) {
        adjustAlgorithmIntervalBasedOnOrderRate();
        lastOrderRateCheck = simulatedTime;
      }

      // Periodic cleanup of metrics to prevent memory buildup
      if (simulatedTime.isAfter(lastMetricsCleanup.plus(METRICS_CLEANUP_INTERVAL))) {
        cleanupOldMetrics();
        lastMetricsCleanup = simulatedTime;
      }

      if (simulatedTime.isAfter(nextPlanningTime)) {
        int ordersToProcess = getOrderBatch(simulatedTime);

        // Track order arrivals for rate calculation (only count new orders, not
        // recalculated ones)
        List<Order> newPendingOrders = realTimeOrderRepository.getPendingOrders();
        if (!newPendingOrders.isEmpty()) {
          orderArrivalHistory.put(simulatedTime, newPendingOrders.size());
        }

        // Request planification only if orders have changed or there are new orders to
        // process
        if (ordersToProcess > 0 && hasOrdersChanged()) {
          requestPlanification();
        } else if (ordersToProcess > 0) {
          log.debug("Orders to process ({}) but no changes detected, skipping planification", ordersToProcess);
        } else {
        }

        nextPlanningTime = nextPlanningTime
            .plus(simulationConfig.getConsumptionInterval());
        log.info("Next planning time: {} (interval: {})", nextPlanningTime,
            simulationConfig.getCurrentAlgorithmInterval());
      }
      updateSystemState(timeStep);

      // Calculate metrics and notify with snapshot
      SimulationMetrics metrics = calculateMetrics();
      PlanificationStatus planificationStatus = planificationService.getPlanificationStatus(sessionId);

      // Update PLG network with current orders from repository
      PLGNetwork updatedNetwork = updatePLGNetworkWithCurrentOrders();

      simulationNotifier
          .notifySnapshot(
              new SimulationSnapshot(LocalDateTime.now(), simulatedTime, updatedNetwork, activeRoutes, metrics,
                  planificationStatus));
      sleep(simulationConfig.getSimulationResolution());
    }

  }

  private int getOrderBatch(LocalDateTime currentDateTime) {
    // For real-time simulation, process all pending orders regardless of their date
    // since orders are added dynamically and should be processed immediately
    List<Order> newOrders = realTimeOrderRepository.getPendingOrders();

    // Only count new pending orders for processing - don't automatically reconsider
    // IN_PROGRESS orders
    // unless there are new orders that might require reoptimization
    int totalOrdersToProcess = newOrders.size();

    if (!newOrders.isEmpty()) {
      log.info("Processing {} pending orders in real-time simulation", newOrders.size());

      // Set new orders to CALCULATING
      newOrders.forEach(order -> {
        realTimeOrderRepository.updateOrderStatus(order.getId(), OrderStatus.CALCULATING);
        log.info("New real-time order {} (date: {}) added to batch for calculation",
            order.getId(), order.getDate());
      });

      // Only when there are new orders, reconsider IN_PROGRESS orders for
      // optimization
      List<Order> inProgressOrders = realTimeOrderRepository.getAllOrders().stream()
          .filter(order -> order.getStatus() == OrderStatus.IN_PROGRESS)
          .toList();

      if (!inProgressOrders.isEmpty()) {
        log.info("Reconsidering {} IN_PROGRESS orders for optimization due to new orders", inProgressOrders.size());

        inProgressOrders.forEach(order -> {
          realTimeOrderRepository.updateOrderStatus(order.getId(), OrderStatus.CALCULATING);
          log.info("Order {} set back to CALCULATING for reconsideration", order.getId());
        });

        totalOrdersToProcess += inProgressOrders.size();
      }
    }

    if (totalOrdersToProcess == 0) {
    }

    return totalOrdersToProcess;
  }

  public void updateTruckState(String truckId, TruckState newState) {
    if (plgNetwork != null) {
      Truck truck = plgNetwork.getTrucks().stream()
          .filter(t -> t.getId().equals(truckId))
          .findFirst()
          .orElse(null);

      if (truck != null) {
        truck.setStatus(newState);
      } else {
      }
    }
  }

  /**
   * Triggers an immediate notification with current state.
   * Used when orders are added to provide immediate feedback.
   */
  public void triggerImmediateUpdate() {
    if (running.get() && plgNetwork != null) {

      SimulationMetrics metrics = calculateMetrics();
      PlanificationStatus planificationStatus = planificationService.getPlanificationStatus(sessionId);
      PLGNetwork updatedNetwork = updatePLGNetworkWithCurrentOrders();

      simulationNotifier.notifySnapshot(
          new SimulationSnapshot(LocalDateTime.now(), simulatedTime, updatedNetwork, activeRoutes, metrics,
              planificationStatus));
    }
  }

  /**
   * Forces immediate planification when new orders are added.
   * This ensures quick response to new order arrivals.
   */
  public void triggerImmediatePlanification() {
    if (running.get() && plgNetwork != null) {

      // Mark any PENDING orders as CALCULATING to ensure they get processed
      List<Order> pendingOrders = realTimeOrderRepository.getAllOrders().stream()
          .filter(order -> order.getStatus() == OrderStatus.PENDING)
          .toList();

      pendingOrders.forEach(order -> {
        realTimeOrderRepository.updateOrderStatus(order.getId(), OrderStatus.CALCULATING);
        log.info("Marked order {} (date: {}) as CALCULATING for immediate processing",
            order.getId(), order.getDate());
      });

      // Also mark IN_PROGRESS orders as CALCULATING for reconsideration
      List<Order> inProgressOrders = realTimeOrderRepository.getAllOrders().stream()
          .filter(order -> order.getStatus() == OrderStatus.IN_PROGRESS)
          .toList();

      inProgressOrders.forEach(order -> {
        realTimeOrderRepository.updateOrderStatus(order.getId(), OrderStatus.CALCULATING);
        log.info("Marked IN_PROGRESS order {} as CALCULATING for reconsideration", order.getId());
      });

      // Check if there are orders to calculate after status update
      boolean hasCalculatingOrders = realTimeOrderRepository.getAllOrders().stream()
          .anyMatch(order -> order.getStatus() == OrderStatus.CALCULATING);

      if (hasCalculatingOrders) {
        requestPlanification();
        triggerImmediateUpdate(); // Also send immediate update
      } else {
      }
    } else {
      log.warn("Cannot trigger immediate planification - running: {}, plgNetwork: {}",
          running.get(), plgNetwork != null);
    }
  }

  public void stop() {
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
      }
      case "RESUME" -> {
        paused.set(false);
        lock.lock();
        try {
          condition.signalAll();
        } finally {
          lock.unlock();
        }
      }
    }
  }

  // Copy necessary methods from SimulationEngine for real-time operations
  private void updateSystemState(Duration timeStep) {
    // Implementation similar to SimulationEngine but working with real-time orders
    if (activeRoutes == null) {
      return;
    }
    if (activeRoutes.getStops().isEmpty() || activeRoutes.getPaths().isEmpty()) {
      return;
    }

    synchronized (routesLock) {
      for (Truck truck : plgNetwork.getTrucks()) {
        if (truck.getStatus() == TruckState.MAINTENANCE || truck.getStatus() == TruckState.BROKEN_DOWN) {
          continue;
        }

        List<Stop> stops = activeRoutes.getStops().getOrDefault(truck.getId(), List.of());
        if (stops.size() <= 1) {
          log.debug("Truck {} has no assigned stops", truck.getId());
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
            truck.setStatus(TruckState.IDLE);
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
      station.reserveCapacity(simulatedTime, glpToRefill, truck.getId(), null);
      truck.setCurrentCapacity(truck.getCurrentCapacity() + glpToRefill);
      log.info("Truck {} refilled {} GLP at station {}", truck.getId(), glpToRefill, station.getName());
    } else {
      log.warn("No GLP available at station {} for truck {}", station.getName(), truck.getId());
    }
  }

  private void handleDeliveryArrival(Truck truck, Stop stop) {
    Order order = realTimeOrderRepository.getAllOrders().stream()
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
      realTimeOrderRepository.updateOrderStatus(order.getId(), order.getStatus());

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
        realTimeOrderRepository.updateOrderStatus(order.getId(), OrderStatus.COMPLETED);

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
    Duration stepDuration = simulationConfig.getSimulationResolution()
        .multipliedBy((long) simulationConfig.getTimeAcceleration());
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

  private void adjustAlgorithmIntervalBasedOnOrderRate() {
    // Calculate order arrival rate over the last window
    LocalDateTime windowStart = simulatedTime.minus(ORDER_RATE_CHECK_WINDOW);

    // Clean up old entries and calculate rate
    orderArrivalHistory.entrySet().removeIf(entry -> entry.getKey().isBefore(windowStart));

    int totalOrdersInWindow = orderArrivalHistory.values().stream().mapToInt(Integer::intValue).sum();
    double windowHours = ORDER_RATE_CHECK_WINDOW.toMinutes() / 60.0;
    double orderArrivalRate = totalOrdersInWindow / windowHours; // orders per hour

    Duration previousInterval = simulationConfig.getCurrentAlgorithmInterval();
    simulationConfig.adjustIntervalBasedOnOrderRate(orderArrivalRate);
    Duration newInterval = simulationConfig.getCurrentAlgorithmInterval();

    if (!previousInterval.equals(newInterval)) {
    }
  }

  private void requestPlanification() {
    // Get current order counts for debugging
    List<Order> allOrders = realTimeOrderRepository.getAllOrders();
    long calculatingOrdersCount = allOrders.stream()
        .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
        .count();
    long pendingOrdersCount = allOrders.stream()
        .filter(order -> order.getStatus() == OrderStatus.PENDING)
        .count();
    long inProgressOrdersCount = allOrders.stream()
        .filter(order -> order.getStatus() == OrderStatus.IN_PROGRESS)
        .count();

    log.info("Real-time planification check - Total orders: {}, PENDING: {}, CALCULATING: {}, IN_PROGRESS: {}",
        allOrders.size(), pendingOrdersCount, calculatingOrdersCount, inProgressOrdersCount);

    // Only request planification if there are orders being calculated
    boolean hasCalculatingOrders = calculatingOrdersCount > 0;

    if (hasCalculatingOrders) {
      lastPlanificationStart = LocalDateTime.now();
      totalPlanificationRequests++;

      // Create updated network with current orders for planification
      PLGNetwork networkForPlanification = updatePLGNetworkWithCurrentOrders();

      // Log the orders being sent to planification
      if (networkForPlanification != null) {
        long networkCalculatingOrders = networkForPlanification.getOrders().stream()
            .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
            .count();
        log.info("Sending PLGNetwork to planification with {} total orders, {} calculating",
            networkForPlanification.getOrders().size(), networkCalculatingOrders);
      }

      eventPublisher.publishEvent(
          new PlanificationRequestEvent(sessionId, networkForPlanification, simulatedTime,
              simulationConfig.getAlgorithmTime(), null));
    } else {
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
    } finally {
      lock.unlock();
    }
  }

  public void onPlanificationResult(Routes routes) {
    synchronized (routesLock) {
      this.activeRoutes = routes;
      // Reset stop indices when new routes are received
      truckCurrentStopIndex.clear();

      // Update truck status based on route assignments
      for (Truck truck : plgNetwork.getTrucks()) {
        if (truck.getStatus() == TruckState.MAINTENANCE || truck.getStatus() == TruckState.BROKEN_DOWN) {
          continue; // Don't change status for trucks in maintenance or broken down
        }

        List<Stop> assignedStops = routes.getStops().getOrDefault(truck.getId(), List.of());

        // Count delivery stops (excluding starting location/depot)
        long deliveryStopCount = assignedStops.stream()
            .filter(stop -> stop.getNode().getType() == NodeType.DELIVERY)
            .count();

        if (deliveryStopCount == 0) {
          truck.setStatus(TruckState.IDLE);
          log.info("Truck {} set to IDLE - has {} total stops but {} delivery stops",
              truck.getId(), assignedStops.size(), deliveryStopCount);
        } else {
          truck.setStatus(TruckState.ACTIVE);
          log.info("Truck {} set to ACTIVE - has {} total stops with {} delivery stops",
              truck.getId(), assignedStops.size(), deliveryStopCount);
        }
      }
    }

    // Track planification time
    if (lastPlanificationStart != null) {
      Duration planificationDuration = Duration.between(lastPlanificationStart, LocalDateTime.now());
      totalPlanificationTime = totalPlanificationTime.plus(planificationDuration);
    }

    List<Order> calculatingOrders = realTimeOrderRepository.getAllOrders().stream()
        .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
        .toList();

    calculatingOrders.forEach(order -> {
      realTimeOrderRepository.updateOrderStatus(order.getId(), OrderStatus.IN_PROGRESS);
    });

  }

  public SimulationStatus getStatus() {
    return new SimulationStatus(
        running.get(),
        paused.get(),
        simulationConfig.getTimeAcceleration());
  }

  private SimulationMetrics calculateMetrics() {
    // Fleet utilization - count only ACTIVE trucks as being used
    int totalTrucks = (int) plgNetwork.getTrucks().stream()
        .filter(t -> t.getStatus() != TruckState.MAINTENANCE && t.getStatus() != TruckState.BROKEN_DOWN)
        .count();
    int activeTrucks = (int) plgNetwork.getTrucks().stream()
        .filter(t -> t.getStatus() == TruckState.ACTIVE)
        .count();
    double fleetUtilization = totalTrucks > 0 ? (double) activeTrucks / totalTrucks * 100 : 0;

    // Fuel efficiency
    double totalFuelConsumed = truckFuelConsumed.values().stream().mapToDouble(Double::doubleValue).sum();
    double totalDistance = truckDistanceTraveled.values().stream().mapToDouble(Double::doubleValue).sum();
    double avgFuelConsumption = totalDistance > 0 ? totalFuelConsumed / totalDistance : 0;

    // Delivery performance
    int totalOrders = realTimeOrderRepository.size();
    int completedOrders = (int) realTimeOrderRepository.getAllOrders().stream()
        .filter(o -> o.getStatus() == OrderStatus.COMPLETED).count();
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
    int totalGLPRequested = realTimeOrderRepository.getAllOrders().stream().mapToInt(Order::getRequestedGLP).sum();
    int totalGLPDelivered = realTimeOrderRepository.getAllOrders().stream().mapToInt(Order::getDeliveredGLP).sum();
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

  /**
   * Updates the PLG network with current orders from the repository.
   * This ensures the frontend receives the latest order state.
   */
  private PLGNetwork updatePLGNetworkWithCurrentOrders() {
    if (plgNetwork == null) {
      return null;
    }

    // Get current orders from repository
    List<Order> currentOrders = realTimeOrderRepository.getAllOrders();

    // Create a new PLG network with updated orders while keeping trucks and
    // stations unchanged
    return new PLGNetwork(
        plgNetwork.getTrucks(),
        plgNetwork.getStations(),
        currentOrders,
        plgNetwork.getIncidents(),
        plgNetwork.getRoadblocks());
  }

  /**
   * Generate a snapshot of current orders for change detection
   * 
   * @return A string representation of orders that can be used to detect changes
   */
  private String generateOrdersSnapshot() {
    List<Order> allOrders = realTimeOrderRepository.getAllOrders();

    // Create a snapshot based on order IDs, statuses, and relevant timestamps
    StringBuilder snapshot = new StringBuilder();
    allOrders.stream()
        .sorted((o1, o2) -> o1.getId().compareTo(o2.getId())) // Sort for consistent comparison
        .forEach(order -> {
          snapshot.append(order.getId())
              .append(":").append(order.getStatus())
              .append(":").append(order.getDate())
              .append(";");
        });

    return snapshot.toString();
  }

  /**
   * Check if orders have changed since last planification
   * 
   * @return true if orders have changed or force replanification is set
   */
  private boolean hasOrdersChanged() {
    String currentSnapshot = generateOrdersSnapshot();
    boolean hasChanged = forceReplanification || !currentSnapshot.equals(lastOrdersSnapshot);

    if (hasChanged) {
      log.debug("Orders changed detected: force={}, snapshot_changed={}",
          forceReplanification, !currentSnapshot.equals(lastOrdersSnapshot));
      lastOrdersSnapshot = currentSnapshot;
      forceReplanification = false; // Reset force flag
    }

    return hasChanged;
  }

  /**
   * Force the next replanification regardless of order changes
   * This can be called when external factors require replanification
   */
  public void forceNextReplanification() {
    this.forceReplanification = true;
  }

  /**
   * Clean up old metrics to prevent memory buildup
   */
  private void cleanupOldMetrics() {
    // Clean up completed orders from orderStartTimes
    List<String> completedOrderIds = realTimeOrderRepository.getAllOrders().stream()
        .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
        .map(Order::getId)
        .toList();
    
    completedOrderIds.forEach(orderStartTimes::remove);
    
    // Clean up customer delivery times - keep only most recent 1000 entries
    if (customerDeliveryTimes.size() > 1000) {
      customerDeliveryTimes.clear();
    }
    
    // Clean up order arrival history older than 1 hour
    LocalDateTime arrivalCutoff = simulatedTime.minusHours(1);
    orderArrivalHistory.entrySet().removeIf(entry -> entry.getKey().isBefore(arrivalCutoff));
    
    log.debug("Cleaned up old metrics - orderStartTimes: {}, customerDeliveryTimes: {}, orderArrivalHistory: {}", 
        orderStartTimes.size(), customerDeliveryTimes.size(), orderArrivalHistory.size());
  }
}
