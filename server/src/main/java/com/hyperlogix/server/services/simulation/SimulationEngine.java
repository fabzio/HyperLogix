package com.hyperlogix.server.services.simulation;

import com.hyperlogix.server.config.Constants;
import com.hyperlogix.server.domain.*;
import com.hyperlogix.server.services.planification.PlanificationService;
import com.hyperlogix.server.services.planification.PlanificationStatus;
import com.hyperlogix.server.services.incident.IncidentManagement;

import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import com.hyperlogix.server.config.Constants;
import com.hyperlogix.server.domain.NodeType;
import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.domain.OrderStatus;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Path;
import com.hyperlogix.server.domain.Point;
import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.domain.Station;
import com.hyperlogix.server.domain.Stop;
import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.domain.TruckState;
import com.hyperlogix.server.features.planification.dtos.PlanificationRequestEvent;
import com.hyperlogix.server.services.planification.PlanificationService;
import com.hyperlogix.server.services.planification.PlanificationStatus;

import lombok.Setter;
import java.util.ArrayList;

public class SimulationEngine implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);
  private static String DEBUG_FILE = "engine_incident_debug.txt";
  private static DateTimeFormatter DEBUG_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  private final ApplicationEventPublisher eventPublisher;
  private final PlanificationService planificationService;
  private final String sessionId;
  private final SimulationConfig simulationConfig;
  private final SimulationNotifier simulationNotifier;
  private final List<Order> orderRepository;

  public enum TruckMaintenanceState {
    NONE,
    TRAVELING_TO_MAINTENANCE, // Camión va hacia mantenimiento pero puede moverse
    IN_MAINTENANCE // Camión está en mantenimiento, no se mueve
  }

  private final Map<String, TruckMaintenanceState> truckMaintenanceStates = new ConcurrentHashMap<>();
  @Setter
  private PLGNetwork plgNetwork;
  private Routes activeRoutes;
  private final Object routesLock = new Object();
  private LocalDateTime nextPlanningTime;
  private LocalDateTime simulatedTime;

  private List<Incident> incidentRepository = new ArrayList<>();

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
  private final Runnable onComplete;
  private int totalPlanificationRequests = 0;
  private Duration totalPlanificationTime = Duration.ZERO;
  private IncidentManagement incidentManager = null;
  private LocalDateTime lastPlanificationStart; // Order arrival rate tracking
  private final Map<LocalDateTime, Integer> orderArrivalHistory = new ConcurrentHashMap<>();
  private LocalDateTime lastOrderRateCheck = null;
  private static final Duration ORDER_RATE_CHECK_WINDOW = Duration.ofMinutes(10);

  private Set<String> trucksInMaintenance = new HashSet<>();

  public SimulationEngine(String sessionId,
      SimulationConfig simulationConfig,
      SimulationNotifier simulationNotifier,
      List<Order> orderRepository,
      ApplicationEventPublisher eventPublisher,
      PlanificationService planificationService, Runnable onComplete) {
    this.sessionId = sessionId;
    this.simulationConfig = simulationConfig;
    this.simulationNotifier = simulationNotifier;
    this.orderRepository = orderRepository;
    this.eventPublisher = eventPublisher;
    this.planificationService = planificationService;
    this.onComplete = onComplete;
  }

  @Override
  public void run() {
    running.set(true);
    log.info("Config K={}, Sa={}, Sc={}, Ta={}",
        simulationConfig.getTimeAcceleration(),
        simulationConfig.getCurrentAlgorithmInterval(),
        simulationConfig.getConsumptionInterval(),
        simulationConfig.getAlgorithmTime());
    simulatedTime = orderRepository.getFirst().getDate().plus(Duration.ofNanos(1));
    nextPlanningTime = simulatedTime;
    lastOrderRateCheck = simulatedTime;

    // Initialize incident manager now that plgNetwork is available
    incidentManager = new IncidentManagement(plgNetwork.getIncidents());

    boolean incidentsOrganized = false;
    while (running.get()) {
      waitIfPaused();

      if (!running.get())
        break;

      // Check if all orders are completed
      if (areAllOrdersCompleted()) {
        break;
      }

      Duration timeStep = simulationConfig.getSimulationResolution()
          .multipliedBy((long) simulationConfig.getTimeAcceleration());

      simulatedTime = simulatedTime.plus(timeStep);

      boolean needsReplaning = checkScheduledMaintenances();

      if (needsReplaning) {
        log.info("Maintenance changes detected, requesting immediate replanning");
        requestPlanification();
      }

      // Check and adjust algorithm interval based on order arrival rate
      if (simulatedTime.isAfter(lastOrderRateCheck.plus(ORDER_RATE_CHECK_WINDOW))) {
        adjustAlgorithmIntervalBasedOnOrderRate();
        lastOrderRateCheck = simulatedTime;
      }

      if (simulatedTime.isAfter(nextPlanningTime)
          && plgNetwork.getTrucks().stream().anyMatch(truck -> truck.getStatus() == TruckState.ACTIVE ||
              truck.getStatus() == TruckState.RETURNING_TO_BASE ||
              truck.getStatus() == TruckState.IDLE)) {
        int newOrderCount = getOrderBatch(simulatedTime);

        // Track order arrivals for rate calculation
        if (newOrderCount > 0) {
          orderArrivalHistory.put(simulatedTime, newOrderCount);
        }
        requestPlanification();
        if (!incidentsOrganized && activeRoutes != null) {
          incidentsOrganized = true;
        }
        this.nextPlanningTime = nextPlanningTime
            .plus(simulationConfig.getConsumptionInterval());
        log.info("Next planning time: {} (interval: {})", nextPlanningTime,
            simulationConfig.getCurrentAlgorithmInterval());
      }

      updateSystemState(timeStep);

      // Calculate metrics and notify with snapshot
      SimulationMetrics metrics = calculateMetrics();
      PlanificationStatus planificationStatus = planificationService.getPlanificationStatus(sessionId);
      simulationNotifier
          .notifySnapshot(
              new SimulationSnapshot(LocalDateTime.now(), simulatedTime, plgNetwork, activeRoutes, metrics,
                  planificationStatus));

      sleep(simulationConfig.getSimulationResolution());
    }
    this.onComplete.run();
  }

  private boolean checkScheduledMaintenances() {
    boolean needsReplanning = false;
    for (Truck truck : plgNetwork.getTrucks()) {
      if (truck.getNextMaintenance() != null) {
        // Si la fecha de mantenimiento llegó y el camión no está ya en mantenimiento
        if (activeRoutes != null
            & (simulatedTime.isEqual(truck.getNextMaintenance()) || simulatedTime.isAfter(truck.getNextMaintenance()))
            && truck.getStatus() != TruckState.MAINTENANCE) {
          boolean wasInRoute = startTruckMaintenance(truck);
          if (wasInRoute) {
            needsReplanning = true;
          }
        }

        // Si ya pasó el día de mantenimiento, terminar el mantenimiento
        if (simulatedTime.isAfter(truck.getNextMaintenance().plusHours(24))
            && truck.getStatus() == TruckState.MAINTENANCE) {
          endTruckMaintenance(truck);
          needsReplanning = true;
        }
      }
    }

    return needsReplanning;
  }

  private boolean startTruckMaintenance(Truck truck) {
    log.info("Starting maintenance for truck {} at {}", truck.getCode(), simulatedTime);

    // Verificar si el camión está en ruta
    boolean wasInRoute = isInRoute(truck);

    if (wasInRoute) {
      log.warn("Truck {} is in route, interrupting current route for maintenance", truck.getCode());

      // Interrumpir ruta actual inmediatamente
      synchronized (routesLock) {
        if (activeRoutes != null) {
          activeRoutes.getStops().put(truck.getId(), List.of());
          activeRoutes.getPaths().put(truck.getId(), List.of());
        }
        truckCurrentStopIndex.remove(truck.getId());
      }
    }
    truck.startMaintenance();
    trucksInMaintenance.add(truck.getId());
    return wasInRoute;
  }

  private void endTruckMaintenance(Truck truck) {
    log.info("Ending maintenance for truck {} at {}", truck.getCode(), simulatedTime);

    // Cambiar estado a activo
    truck.endMaintenance(simulatedTime);
    trucksInMaintenance.remove(truck.getId());
  }

  private boolean isInRoute(Truck truck) {
    List<Stop> stops = activeRoutes.getStops().getOrDefault(truck.getId(), List.of());
    if (stops.size() <= 1)
      return false;
    return true;
  }

  private boolean areAllOrdersCompleted() {
    return orderRepository.stream()
        .allMatch(order -> order.getStatus() == OrderStatus.COMPLETED);
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
      case "DESACCELERATE" -> {
        double newAcceleration = Math.max(1.0, simulationConfig.getTimeAcceleration() / 2);
        simulationConfig.setTimeAcceleration(newAcceleration);
      }
      case "ACCELERATE" -> {
        double newAcceleration = simulationConfig.getTimeAcceleration() * 2;
        simulationConfig.setTimeAcceleration(newAcceleration);
      }
    }
  }

  private void updateSystemState(Duration timeStep) {
    if (activeRoutes == null) {
      return;
    }
    if (activeRoutes.getStops().isEmpty() || activeRoutes.getPaths().isEmpty()) {
      return;
    }
    synchronized (routesLock) {
      for (Truck truck : plgNetwork.getTrucks()) {
        if (truck.getStatus() == TruckState.BROKEN_DOWN) {
          Incident recoveredIncident = incidentRepository.stream()
              .filter(incident -> incident.getTruckCode().equals(truck.getCode()))
              .findFirst()
              .orElse(null);

          if (recoveredIncident != null) {
            boolean recovered = incidentManager.handleMaintenanceDelay(truck, simulatedTime, recoveredIncident);
          }
          continue;
        }

        if (truck.getStatus() == TruckState.MAINTENANCE) {
          Incident recoveredIncident = incidentRepository.stream()
              .filter(incident -> incident.getTruckCode().equals(truck.getCode()))
              .findFirst()
              .orElse(null);

          if (recoveredIncident != null) {
            if (recoveredIncident.getExpectedRecovery().isAfter(simulatedTime)) {
              truck.endMaintenance(simulatedTime);
              recoveredIncident.setStatus(IncidentStatus.RESOLVED);
            }
          }
          continue;
        }

        // Get assigned route for this truck
        List<Stop> stops = activeRoutes.getStops().getOrDefault(truck.getId(), List.of());
        if (stops.size() <= 1) {
          log.trace("Truck {} has no assigned stops", truck.getId());
          continue;
        }

        List<Path> paths = activeRoutes.getPaths().getOrDefault(truck.getId(), List.of());
        int currentStopIndex = truckCurrentStopIndex.getOrDefault(truck.getId(), 0);

        if (currentStopIndex >= stops.size()) {
          log.debug("Truck {} has completed all stops", truck.getId());
          continue;
        }

        Stop currentStop = stops.get(currentStopIndex);

        log.trace("Truck {} processing - currentStopIndex: {}, stops: {}, target stop: {} at {}",
            truck.getId(), currentStopIndex, stops.size(),
            currentStop.getNode().getId(), currentStop.getArrivalTime());

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

        // Update truck location during travel - truck should be traveling TO the
        // current target stop
        // Only update if truck hasn't arrived yet and there's a previous stop to travel
        // from
        if (currentStopIndex > 0 && currentStopIndex < stops.size() &&
            simulatedTime.isBefore(currentStop.getArrivalTime())) {
          log.trace("Truck {} traveling to stop {}, current time: {}, arrival time: {}",
              truck.getId(), currentStop.getNode().getId(), simulatedTime, currentStop.getArrivalTime());
          updateTruckLocationDuringTravel(truck, stops, paths, currentStopIndex - 1);
        } else {
          log.trace("Truck {} not traveling - stopIndex: {}, totalStops: {}, currentTime: {}, arrivalTime: {}",
              truck.getId(), currentStopIndex, stops.size(), simulatedTime, currentStop.getArrivalTime());
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
    } else if (stop.getNode().getType() == NodeType.INCIDENT) {
      handleIncidentArrival(truck, stop);
    }
  }

  /**
   * Maneja la llegada de un camión a un nodo de incidente
   * Realiza la transferencia de carga entre camiones y actualiza los estados
   */
  private void handleIncidentArrival(Truck truck, Stop stop) {
    Incident incident = incidentRepository.stream()
        .filter(i -> i.getId().equals(stop.getNode().getId()))
        .findFirst().orElse(null);

    if (incident == null) {
      return;
    }

    Truck accidentedTruck = plgNetwork.getTrucks().stream()
        .filter(t -> t.getCode().equals(incident.getTruckCode()))
        .findFirst().orElse(null);

    if (accidentedTruck == null) {
      return;
    }

    int transferAmount = Math.min(
        accidentedTruck.getCurrentCapacity(), // Lo que tiene el camión accidentado
        truck.getMaxCapacity() - truck.getCurrentCapacity() // Espacio disponible en el camión rescatista
    );

    if (transferAmount <= 0) {
      return;
    }

    // 4. Realizar la transferencia de carga
    accidentedTruck.setCurrentCapacity(accidentedTruck.getCurrentCapacity() - transferAmount);
    truck.setCurrentCapacity(truck.getCurrentCapacity() + transferAmount);
    incident.setFuel(accidentedTruck.getCurrentCapacity()); // Actualizar combustible restante en el incidente

    writeDebugLog(String.format("Transferencia completada: %d unidades de GLP transferidas del camión %s al camión %s",
        transferAmount, accidentedTruck.getCode(), truck.getCode()));

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

    if (truck.getStatus() == TruckState.RETURNING_TO_BASE) {
      truck.setStatus(TruckState.IDLE);
      log.info("Truck {} returned to base and is now IDLE", truck.getId());
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

        // Calculate delivery time
        LocalDateTime startTime = orderStartTimes.get(order.getId());
        if (startTime != null) {
          Duration deliveryTime = Duration.between(startTime, simulatedTime);
          customerDeliveryTimes.put(order.getClientId(), deliveryTime);
        }

        log.info("Order {} completed", order.getId());
        order.setStatus(OrderStatus.COMPLETED);
      }
    } else {
      log.warn("Truck {} has no capacity to deliver to order {}", truck.getId(), order.getId());
    }
  }

  private void updateTruckLocationDuringTravel(Truck truck, List<Stop> stops, List<Path> paths, int currentStopIndex) {
    if (currentStopIndex >= stops.size() - 1 || currentStopIndex >= paths.size()) {
      return; // No more paths to travel
    }

    Stop fromStop = stops.get(currentStopIndex);
    Path currentPath = paths.get(currentStopIndex);

    // Calculate time elapsed since leaving the fromStop
    Duration timeElapsed = Duration.between(fromStop.getArrivalTime(), simulatedTime);
    double hoursElapsed = timeElapsed.toSeconds() / 3600.0;

    // Don't move backward in time
    if (hoursElapsed < 0) {
      return;
    }

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
    Point interpolatedPosition = interpolateAlongPath(currentPath.points(), progress); // Update truck location
    truck.setLocation(interpolatedPosition);

    // Check for incidents between 5% and 35% progress of current path
    if (progress >= 0.05 && progress <= 0.35) {
      Incident detectedIncident = incidentManager.checkAndHandleIncident(truck, simulatedTime);
      if (detectedIncident != null) {
        changeOrdersState(truck.getId(), OrderStatus.CALCULATING);
        incidentRepository.add(detectedIncident);
      }
    }
  }

  private void changeOrdersState(String truckId, OrderStatus newStatus) {
    // Cambiar estado A todas las órdenes asignadas actualmente a este camión
    List<Stop> assignedStops = activeRoutes.getStops().getOrDefault(truckId, List.of());
    for (Stop stop : assignedStops) {
      if (stop.getNode().getType() == NodeType.DELIVERY) {
        Order order = orderRepository.stream()
            .filter(o -> o.getId().equals(stop.getNode().getId()))
            .findFirst().orElse(null);
        if (order != null) {
          order.setStatus(newStatus);
        }
      }
    }
    // Las órdenes deben ser prioridad para este camión en la próxima planificación
    // (El planificador debe consultar truckPriorityOrders para forzar la
    // asignación)
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

  private int getOrderBatch(LocalDateTime currenDateTime) {
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
          .filter(order -> order.getStatus() == OrderStatus.IN_PROGRESS || order.getStatus() == OrderStatus.DELAYED)
          .toList();

      inProgressOrders.forEach(order -> {
        order.setStatus(OrderStatus.CALCULATING);
        log.info("Order {} set back to CALCULATING due to new batch", order.getId());
      });
    }

    return newOrders.size();
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
    boolean hasCalculatingOrders = orderRepository.stream()
        .anyMatch(order -> order.getStatus() == OrderStatus.CALCULATING);

    if (hasCalculatingOrders) {
      lastPlanificationStart = LocalDateTime.now();
      totalPlanificationRequests++;

      List<Incident> immobilizedIncidents = incidentRepository.stream()
          .filter(incident -> incident.getStatus() == IncidentStatus.IMMOBILIZED)
          .toList();

      //
      eventPublisher.publishEvent(
          new PlanificationRequestEvent(sessionId, plgNetwork, simulatedTime, simulationConfig.getAlgorithmTime(),
              immobilizedIncidents));
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
    if (routes == null || routes.getStops().isEmpty()) {
      List<Order> calculatingOrders = orderRepository.stream()
          .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
          .toList();
      calculatingOrders.forEach(order -> {
        order.setStatus(OrderStatus.PENDING);
      });
      return;
    }

    synchronized (routesLock) {
      // Debug info antes del cambio
      for (Truck truck : plgNetwork.getTrucks()) {
        if (truck.getStatus() == TruckState.MAINTENANCE) {
          List<Stop> oldStops = activeRoutes != null ? activeRoutes.getStops().getOrDefault(truck.getId(), List.of())
              : List.of();
          Integer oldIndex = truckCurrentStopIndex.get(truck.getId());

          log.info("BEFORE MAINTENANCE - Truck {} currentStopIndex: {}, current stops: {}",
              truck.getId(), oldIndex, oldStops.size());
        }
      }

      this.activeRoutes = routes;
      // Reset stop indices cuando se reciben nuevas rutas
      truckCurrentStopIndex.clear();

      log.info("Received routes for {} trucks, {} total stops, {} total paths",
          routes.getStops().size(),
          routes.getStops().values().stream().mapToInt(List::size).sum(),
          routes.getPaths().values().stream().mapToInt(List::size).sum());

      for (Truck truck : plgNetwork.getTrucks()) {
        if (truck.getStatus() == TruckState.BROKEN_DOWN) {
          continue; // No cambiar estado para camiones averiados
        }

        List<Stop> assignedStops = routes.getStops().getOrDefault(truck.getId(), List.of());

        if (truck.getStatus() == TruckState.MAINTENANCE) {
          // Debug después del cambio
          Integer newIndex = truckCurrentStopIndex.get(truck.getId());

          log.info("AFTER MAINTENANCE - Truck {} currentStopIndex: {}, new stops: {}",
              truck.getId(), newIndex, assignedStops.size());

          if (assignedStops.size() > 1) {
            // CAMBIO: NO cambiar a ACTIVE, mantener MAINTENANCE
            // El camión debe seguir en mantenimiento hasta que termine
            log.info("Maintenance truck {} assigned route to station (but staying in MAINTENANCE state)",
                truck.getId());

            // Log detalles de la ruta
            StringBuilder routeInfo = new StringBuilder();
            for (int i = 1; i < assignedStops.size(); i++) {
              Stop stop = assignedStops.get(i);
              routeInfo.append(stop.getNode().getType()).append(":").append(stop.getNode().getId());
              if (i < assignedStops.size() - 1)
                routeInfo.append(" -> ");
            }
            log.info("AFTER MAINTENANCE - Truck {} route: {}", truck.getId(), routeInfo.toString());

            log.info("AFTER MAINTENANCE - Truck {} ALL stops with times: {}", truck.getId(),
                assignedStops.stream()
                    .map(s -> s.getNode().getType() + ":" + s.getNode().getId() + "@" + s.getArrivalTime()).toList());

          } else {
            // Sin ruta asignada, mantener en mantenimiento
            log.info("Maintenance truck {} has no route assigned", truck.getId());
          }
          continue; // IMPORTANTE: No procesar más, mantener estado MAINTENANCE
        }

        // Para camiones que NO están en mantenimiento
        if (assignedStops.size() <= 1) {
          truck.setStatus(TruckState.IDLE);
          log.debug("Truck {} set to IDLE - no assigned routes", truck.getId());
        } else {
          truck.setStatus(TruckState.ACTIVE);

          // Log detalles de la ruta para debugging
          StringBuilder routeInfo = new StringBuilder();
          for (int i = 1; i < assignedStops.size(); i++) { // Saltar primera parada (ubicación inicial)
            Stop stop = assignedStops.get(i);
            routeInfo.append(stop.getNode().getType()).append(":").append(stop.getNode().getId());
            if (i < assignedStops.size() - 1)
              routeInfo.append(" -> ");
          }
          log.info("Truck {} set to ACTIVE - route: {}", truck.getId(), routeInfo.toString());
        }
      }
    }

    // Track planification time
    if (lastPlanificationStart != null) {
      Duration planificationDuration = Duration.between(lastPlanificationStart, LocalDateTime.now());
      totalPlanificationTime = totalPlanificationTime.plus(planificationDuration);
    }

    // Only change status for CALCULATING orders to IN_PROGRESS
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

  // metodo para debuggear
  private void writeDebugLog(String message) {
    try (FileWriter writer = new FileWriter(DEBUG_FILE)) {
      String timestamp = LocalDateTime.now().format(DEBUG_TIME_FORMAT);
      writer.write("[" + timestamp + "] SIM - " + message + "\n");
    } catch (IOException e) {
      System.err.println("Error writing debug log: " + e.getMessage());
    }
  }

}
