package com.hyperlogix.server.services.simulation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import java.util.stream.Collectors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;

import com.hyperlogix.server.config.Constants;
import com.hyperlogix.server.domain.Edge;
import com.hyperlogix.server.domain.Node;
import com.hyperlogix.server.domain.NodeType;
import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.domain.OrderStatus;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Path;
import com.hyperlogix.server.domain.Point;
import com.hyperlogix.server.domain.Roadblock;
import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.domain.Station;
import com.hyperlogix.server.domain.Stop;
import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.domain.TruckState;
import com.hyperlogix.server.features.planification.dtos.PlanificationRequestEvent;
import com.hyperlogix.server.services.planification.PlanificationService;
import com.hyperlogix.server.services.planification.PlanificationStatus;

import lombok.Setter;

public class SimulationEngine implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);

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
  private LocalDateTime lastPlanificationStart;

  private Set<String> trucksInMaintenance = new HashSet<>();

  private static final Duration ORDER_PROCESSING_TIMEOUT = Duration.ofSeconds(80);
  private final Map<String, LocalDateTime> orderCalculatingStartTime = new ConcurrentHashMap<>();
  private int consecutiveFailures = 0;

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

    simulatedTime = orderRepository.getFirst().getDate().plus(Duration.ofNanos(1));
    nextPlanningTime = simulatedTime;

    while (running.get()) {
      try {
        waitIfPaused();

        if (!running.get()) {
          log.info("Simulation stopped by external request");
          break;
        }
        if (areAllOrdersCompleted()) {
          log.info("All orders completed, ending simulation at {}", simulatedTime);
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

        if (simulatedTime.isAfter(nextPlanningTime)) {
          logDetailedState("BEFORE_PLANNING");

          checkBlockedOrdersForUnblocking(simulatedTime);

          // Verificar y desbloquear órdenes que ya no están afectadas por roadblocks
          checkAndUnblockOrders(simulatedTime);

          getOrderBatch(simulatedTime);

          requestPlanification();
          nextPlanningTime = nextPlanningTime
              .plus(simulationConfig.getConsumptionInterval());

          log.info("Next planning time: {} (interval: {})", nextPlanningTime,
              simulationConfig.getCurrentAlgorithmInterval());

          logDetailedState("AFTER_PLANNING_REQUEST");
        }

        var truckProgress = updateSystemState(timeStep);

        // Calculate metrics and notify with snapshot
        SimulationMetrics metrics = calculateMetrics();
        PlanificationStatus planificationStatus = planificationService.getPlanificationStatus(sessionId);

        simulationNotifier
            .notifySnapshot(
                new SimulationSnapshot(LocalDateTime.now(), simulatedTime, plgNetwork, activeRoutes, truckProgress,
                    metrics,
                    planificationStatus));

        sleep(simulationConfig.getSimulationResolution());

      } catch (Exception e) {
        log.error("FATAL ERROR in simulation loop at simulated time {}: {}", simulatedTime, e.getMessage(), e);

        // Log complete system state on error
        logSystemStateOnError();

        // Intentar recuperación
        try {
        } catch (Exception recoveryError) {
          log.error("Recovery failed: {}", recoveryError.getMessage(), recoveryError);
          break;
        }
      }
    }

    log.info("=== SIMULATION END === Final time: {}, Total orders: {}, Active routes: {}",
        simulatedTime, orderRepository.size(), activeRoutes != null ? activeRoutes.getStops().size() : 0);

    MDC.clear();
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
        int calculatingCount = (int) orderRepository.stream()
            .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
            .count();
      }
      case "ACCELERATE" -> {
        double newAcceleration = simulationConfig.getTimeAcceleration() * 2;
        simulationConfig.setTimeAcceleration(newAcceleration);
      }
    }
  }

  private HashMap<String, Double> updateSystemState(Duration timeStep) {
    if (activeRoutes == null) {
      return null;
    }
    if (activeRoutes.getStops().isEmpty() || activeRoutes.getPaths().isEmpty()) {
      return null;
    }
    HashMap<String, Double> truckProgress = new HashMap<>();

    synchronized (routesLock) {
      for (Truck truck : plgNetwork.getTrucks()) {
        if (truck.getStatus() == TruckState.BROKEN_DOWN) {
          continue;
        }

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
        if (currentStopIndex > 0 && currentStopIndex < stops.size() &&
            simulatedTime.isBefore(currentStop.getArrivalTime())) {
          log.trace("Truck {} traveling to stop {}, current time: {}, arrival time: {}",
              truck.getId(), currentStop.getNode().getId(), simulatedTime, currentStop.getArrivalTime());
          double progress = updateTruckLocationDuringTravel(truck, stops, paths, currentStopIndex - 1);
          truckProgress.put(truck.getId(), progress);
        } else {
          log.trace("Truck {} not traveling - stopIndex: {}, totalStops: {}, currentTime: {}, arrivalTime: {}",
              truck.getId(), currentStopIndex, stops.size(), simulatedTime, currentStop.getArrivalTime());
        }
      }
    }
    return truckProgress;
  }

  private void handleStopArrival(Truck truck, Stop stop) {
    log.info("Truck {} arrived at stop {} at {}", truck.getId(), stop.getNode().getId(), simulatedTime);

    if (stop.getNode().getType() == NodeType.STATION) {
      handleStationArrival(truck, stop);
    } else if (stop.getNode().getType() == NodeType.DELIVERY) {
      handleDeliveryArrival(truck, stop);
    }
    stop.setArrived(true);
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

  private double updateTruckLocationDuringTravel(Truck truck, List<Stop> stops, List<Path> paths, int pathIndex) {
    if (pathIndex < 0 || pathIndex >= paths.size() || pathIndex >= stops.size() - 1) {
      return 0; // Invalid path index or no more paths to travel
    }

    Stop fromStop = stops.get(pathIndex);
    Stop toStop = stops.get(pathIndex + 1);
    Path currentPath = paths.get(pathIndex);

    // Calculate time elapsed since leaving the fromStop
    Duration timeElapsed = Duration.between(fromStop.getArrivalTime(), simulatedTime);
    double hoursElapsed = timeElapsed.toSeconds() / 3600.0;

    // Don't move backward in time
    if (hoursElapsed < 0) {
      return 0;
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
    Point interpolatedPosition = interpolateAlongPath(currentPath.points(), progress);

    // Update truck location
    truck.setLocation(interpolatedPosition);

    log.debug("Truck {} traveling from stop {} to stop {} - position: ({}, {}) - progress: {:.2f}%, fuel: {:.2f}gal",
        truck.getId(), fromStop.getNode().getId(), toStop.getNode().getId(),
        interpolatedPosition.x(), interpolatedPosition.y(), progress * 100, newFuelLevel);
    return progress;
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

  /**
   * Verifica si una orden está bloqueada por roadblocks activos durante su tiempo
   * de disponibilidad
   */
  private boolean isOrderBlocked(Order order, LocalDateTime currentDateTime) {
    if (plgNetwork.getRoadblocks() == null || plgNetwork.getRoadblocks().isEmpty()) {
      return false;
    }

    LocalDateTime orderStartTime = order.getDate();
    LocalDateTime orderEndTime = order.getMaxDeliveryDate();

    for (Roadblock roadblock : plgNetwork.getRoadblocks()) {
      // Verificar si el roadblock está activo durante el tiempo de disponibilidad de
      // la orden
      if (isRoadblockActiveInTimeRange(roadblock, orderStartTime, orderEndTime)) {
        // Verificar si la orden intersecta geográficamente con el roadblock
        if (orderIntersectsWithRoadblock(order, roadblock)) {
          log.info("Order {} is blocked by roadblock active from {} to {}",
              order.getId(), roadblock.start(), roadblock.end());
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Marca una orden como bloqueada y establece el tiempo de fin del bloqueo
   */
  private void blockOrder(Order order, LocalDateTime currentDateTime) {
    if (plgNetwork.getRoadblocks() == null || plgNetwork.getRoadblocks().isEmpty()) {
      return;
    }

    LocalDateTime orderStartTime = order.getDate();
    LocalDateTime orderEndTime = order.getMaxDeliveryDate();
    LocalDateTime earliestBlockEnd = null;

    for (Roadblock roadblock : plgNetwork.getRoadblocks()) {
      // Verificar si el roadblock está activo durante el tiempo de disponibilidad de
      // la orden
      if (isRoadblockActiveInTimeRange(roadblock, orderStartTime, orderEndTime)) {
        // Verificar si la orden intersecta geográficamente con el roadblock
        if (orderIntersectsWithRoadblock(order, roadblock)) {
          // Encontrar el tiempo de fin más temprano entre todos los roadblocks que
          // bloquean esta orden
          if (earliestBlockEnd == null || roadblock.end().isBefore(earliestBlockEnd)) {
            earliestBlockEnd = roadblock.end();
          }
        }
      }
    }

    if (earliestBlockEnd != null) {
      order.setStatus(OrderStatus.BLOCKED);
      order.setBlockEndTime(earliestBlockEnd);
      log.info("Order {} set to BLOCKED due to active roadblocks, will be unblocked at {}",
          order.getId(), earliestBlockEnd);
    }
  }

  /**
   * Verifica si un roadblock está activo durante un rango de tiempo dado
   * Solo considera el roadblock activo si ya ha comenzado en el tiempo actual de
   * simulación
   */
  private boolean isRoadblockActiveInTimeRange(Roadblock roadblock, LocalDateTime startTime, LocalDateTime endTime) {
    // El roadblock debe haber comenzado ya (startTime del roadblock <= tiempo
    // actual de simulación)
    // y debe tener superposición temporal con el período de disponibilidad de la
    // orden
    boolean roadblockHasStarted = !roadblock.start().isAfter(simulatedTime);
    boolean hasTemporalOverlap = !(roadblock.end().isBefore(startTime) || roadblock.start().isAfter(endTime));

    return roadblockHasStarted && hasTemporalOverlap;
  }

  /**
   * Verifica si una orden intersecta geográficamente con los segmentos bloqueados
   * de un roadblock
   */
  private boolean orderIntersectsWithRoadblock(Order order, Roadblock roadblock) {
    Point orderLocation = order.getLocation();
    Set<Edge> blockedEdges = roadblock.parseRoadlock();

    // Distancia mínima para considerar que una orden está afectada por un bloqueo
    // (en km)
    double INTERSECTION_THRESHOLD = 0.5; // 500 metros

    for (Edge blockedEdge : blockedEdges) {
      double distance = calculateDistancePointToSegment(orderLocation, blockedEdge);
      if (distance <= INTERSECTION_THRESHOLD) {
        log.debug("Order {} intersects with blocked edge {} - distance: {} km",
            order.getId(), blockedEdge, distance);
        return true;
      }
    }

    return false;
  }

  /**
   * Calcula la distancia de un punto a un segmento de línea
   */
  private double calculateDistancePointToSegment(Point point, Edge segment) {
    Point A = segment.from();
    Point B = segment.to();

    // Vector AB
    double ABx = B.x() - A.x();
    double ABy = B.y() - A.y();

    // Vector AP
    double APx = point.x() - A.x();
    double APy = point.y() - A.y();

    // Producto escalar AB · AP
    double AB_AP = ABx * APx + ABy * APy;

    // Longitud cuadrada de AB
    double AB_squared = ABx * ABx + ABy * ABy;

    if (AB_squared == 0) {
      // A y B son el mismo punto, calcular distancia a A
      return Math.sqrt((point.x() - A.x()) * (point.x() - A.x()) + (point.y() - A.y()) * (point.y() - A.y()));
    }

    // Parámetro t para la proyección del punto sobre la línea AB
    double t = AB_AP / AB_squared;

    Point closestPoint;
    if (t < 0) {
      // La proyección está antes de A
      closestPoint = A;
    } else if (t > 1) {
      // La proyección está después de B
      closestPoint = B;
    } else {
      // La proyección está en el segmento AB
      closestPoint = new Point(A.x() + t * ABx, A.y() + t * ABy);
    }

    // Calcular distancia euclidiana
    double dx = point.x() - closestPoint.x();
    double dy = point.y() - closestPoint.y();
    return Math.sqrt(dx * dx + dy * dy);
  }

  /**
   * Verifica y desbloquea órdenes que ya no están afectadas por roadblocks
   */
  private void checkAndUnblockOrders(LocalDateTime currentDateTime) {
    List<Order> blockedOrders = orderRepository.stream()
        .filter(order -> order.getStatus() == OrderStatus.BLOCKED)
        .toList();

    for (Order order : blockedOrders) {
      if (!isOrderBlocked(order, currentDateTime)) {
        order.setStatus(OrderStatus.PENDING);
        order.setBlockEndTime(null);
        Duration newLimit = Duration.between(simulatedTime, order.getMaxDeliveryDate());
        order.setDeliveryLimit(newLimit.plus(Duration.ofHours(8)));
        log.info("Order {} unblocked - no longer affected by roadblocks", order.getId());
      }
    }
  }

  private void checkBlockedOrdersForUnblocking(LocalDateTime currentDateTime) {
    List<Order> blockedOrders = orderRepository.stream()
        .filter(order -> order.getStatus() == OrderStatus.BLOCKED)
        .filter(order -> order.getBlockEndTime() != null)
        .toList();

    for (Order order : blockedOrders) {
      // Si el tiempo actual es mayor o igual al tiempo de fin del bloqueo,
      // desbloquear la orden
      if (!currentDateTime.isBefore(order.getBlockEndTime())) {
        order.setStatus(OrderStatus.CALCULATING);
        order.setBlockEndTime(null);
        Duration newLimit = Duration.between(simulatedTime, order.getDate());
        order.setDeliveryLimit(newLimit.plus(Duration.ofHours(8)));
        orderCalculatingStartTime.put(order.getId(), simulatedTime);
        log.info("Order {} automatically unblocked at {} - moved to CALCULATING",
            order.getId(), currentDateTime);
      }
    }
  }

  private int getOrderBatch(LocalDateTime currentDateTime) {
    // Procesar normalmente con límites
    List<Order> candidateOrders = orderRepository.stream()
        .filter(order -> order.getDate().isBefore(currentDateTime) &&
            order.getStatus() == OrderStatus.PENDING)
        .toList();

    List<Order> newOrders = new ArrayList<>();

    if (!candidateOrders.isEmpty()) {
      for (Order order : candidateOrders) {
        // Verificar si la orden está bloqueada por roadblocks activos
        if (isOrderBlocked(order, currentDateTime)) {
          blockOrder(order, currentDateTime);
        } else {
          order.setStatus(OrderStatus.CALCULATING);
          newOrders.add(order);
          log.info("New order {} added to batch for calculation", order.getId());
        }
      }

      List<Order> inProgressOrders = orderRepository.stream()
          .filter(order -> order.getStatus() == OrderStatus.IN_PROGRESS)
          .sorted(Comparator.comparing(Order::getMaxDeliveryDate, Comparator.nullsLast(Comparator.reverseOrder())))
          .limit(5)
          .toList();
      inProgressOrders.forEach(order -> {
        order.setStatus(OrderStatus.CALCULATING);
        orderCalculatingStartTime.put(order.getId(), simulatedTime);
        log.info("IN_PROGRESS order {} added to batch for recalculation (latest delivery date)", order.getId());
      });

      // Add these IN_PROGRESS orders to the newOrders list
      newOrders.addAll(inProgressOrders);
    }

    return newOrders.size();

  }

  private void handleCalculatingOrdersOverflow() {
    log.warn("Handling calculating orders overflow");

    List<Order> calculatingOrders = orderRepository.stream()
        .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
        .toList();

    // Verificar órdenes que llevan mucho tiempo calculando
    LocalDateTime now = LocalDateTime.now();
    List<Order> stuckOrders = calculatingOrders.stream()
        .filter(order -> {
          LocalDateTime startTime = orderCalculatingStartTime.get(order.getId());
          return startTime != null &&
              Duration.between(startTime, now).compareTo(ORDER_PROCESSING_TIMEOUT) > 0;
        })
        .toList();

    if (!stuckOrders.isEmpty()) {
      log.warn("Found {} stuck orders, applying emergency processing", stuckOrders.size());

      // Dividir órdenes: las más urgentes se procesan con algoritmo simple,
      // las menos urgentes se postponen
      List<Order> urgentOrders = stuckOrders.stream()
          .filter(this::isUrgentOrder)
          .limit(10) // Máximo 10 órdenes urgentes
          .toList();

      List<Order> postponableOrders = stuckOrders.stream()
          .filter(order -> !urgentOrders.contains(order))
          .toList();

      // Procesar órdenes urgentes con algoritmo greedy
      if (!urgentOrders.isEmpty()) {
        processOrdersWithGreedyAlgorithm(urgentOrders);
      }

      // Postponer órdenes no urgentes
      postponeOrders(postponableOrders);

      // Limpiar timers
      stuckOrders.forEach(order -> orderCalculatingStartTime.remove(order.getId()));
    }
  }

  private boolean isUrgentOrder(Order order) {
    // Considerar urgente si:
    // 1. La orden es de hace más de 2 horas simuladas
    // 2. La cantidad es pequeña (más fácil de manejar)

    Duration orderAge = Duration.between(order.getDate(), simulatedTime);
    boolean isOld = orderAge.compareTo(Duration.ofHours(2)) > 0;
    boolean isSmallOrder = order.getRequestedGLP() <= 500;

    return isOld || isSmallOrder;
  }

  private void processOrdersWithGreedyAlgorithm(List<Order> orders) {
    log.info("Processing {} orders with emergency greedy algorithm", orders.size());

    Routes emergencyRoutes = new Routes(new HashMap<>(), new HashMap<>(), 0.0);

    // Obtener camiones disponibles
    List<Truck> availableTrucks = plgNetwork.getTrucks().stream()
        .filter(truck -> truck.getStatus() == TruckState.IDLE ||
            truck.getStatus() == TruckState.ACTIVE)
        .filter(truck -> truck.getCurrentFuel() > truck.getFuelCapacity() * 0.1)
        .toList();

    if (availableTrucks.isEmpty()) {
      log.warn("No available trucks for emergency processing, postponing orders");
      postponeOrders(orders);
      return;
    }

    // Asignación simple: un camión por orden (si es posible)
    int processedOrders = 0;
    for (int i = 0; i < Math.min(orders.size(), availableTrucks.size()); i++) {
      Order order = orders.get(i);
      Truck truck = availableTrucks.get(i);

      if (truck.getCurrentCapacity() >= order.getRequestedGLP() - order.getDeliveredGLP()) {
        createDirectRoute(truck, order, emergencyRoutes);
        order.setStatus(OrderStatus.IN_PROGRESS);
        processedOrders++;
        log.info("Emergency route created: Truck {} -> Order {}", truck.getId(), order.getId());
      }
    }

    // Aplicar rutas de emergencia
    if (processedOrders > 0) {
      synchronized (routesLock) {
        // Merger con rutas existentes si las hay
        if (activeRoutes != null) {
          mergeRoutes(activeRoutes, emergencyRoutes);
        } else {
          activeRoutes = emergencyRoutes;
        }
      }
      log.info("Applied emergency routes for {} orders", processedOrders);
    }

    // Órdenes no procesadas se postponen
    List<Order> unprocessedOrders = orders.subList(processedOrders, orders.size());
    if (!unprocessedOrders.isEmpty()) {
      postponeOrders(unprocessedOrders);
    }
  }

  private void createDirectRoute(Truck truck, Order order, Routes routes) {
    List<Stop> stops = new ArrayList<>();
    List<Path> paths = new ArrayList<>();

    // Stop inicial (posición actual del camión)
    Node initialNode = new Node(truck.getId(), "Current Position", NodeType.STATION, truck.getLocation());
    Stop initialStop = new Stop(initialNode, simulatedTime);
    stops.add(initialStop);

    // Si el camión necesita recargar, ir a la estación más cercana primero
    if (truck.getCurrentCapacity() < order.getRequestedGLP() - order.getDeliveredGLP()) {
      Station nearestStation = findNearestStation(truck.getLocation());
      if (nearestStation != null) {
        Node stationNode = new Node(nearestStation);
        Stop stationStop = new Stop(stationNode, simulatedTime.plusMinutes(30));
        stops.add(stationStop);

        // Path simple a la estación
        Path pathToStation = new Path(List.of(truck.getLocation(), nearestStation.getLocation()),
            calculateSimpleDistance(truck.getLocation(), nearestStation.getLocation()));
        paths.add(pathToStation);
      }
    }

    // Stop de entrega
    Node deliveryNode = new Node(order);
    Stop deliveryStop = new Stop(deliveryNode, simulatedTime.plusMinutes(60));
    stops.add(deliveryStop);

    // Path a la entrega
    Point fromLocation = stops.size() > 1 ? stops.get(stops.size() - 2).getNode().getLocation() : truck.getLocation();
    Path pathToDelivery = new Path(List.of(fromLocation, order.getLocation()),
        calculateSimpleDistance(fromLocation, order.getLocation()));
    paths.add(pathToDelivery);

    routes.getStops().put(truck.getId(), stops);
    routes.getPaths().put(truck.getId(), paths);
  }

  private int calculateSimpleDistance(Point from, Point to) {
    // Distancia Manhattan simple para el fallback
    return (int) (Math.abs(to.x() - from.x()) + Math.abs(to.y() - from.y()));
  }

  private Station findNearestStation(Point location) {
    return plgNetwork.getStations().stream()
        .min((s1, s2) -> {
          double dist1 = Math.sqrt(Math.pow(s1.getLocation().x() - location.x(), 2) +
              Math.pow(s1.getLocation().y() - location.y(), 2));
          double dist2 = Math.sqrt(Math.pow(s2.getLocation().x() - location.x(), 2) +
              Math.pow(s2.getLocation().y() - location.y(), 2));
          return Double.compare(dist1, dist2);
        })
        .orElse(null);
  }

  private void mergeRoutes(Routes existingRoutes, Routes newRoutes) {
    // Simple merge: agregar nuevas rutas a las existentes
    newRoutes.getStops().forEach((truckId, stops) -> {
      if (!existingRoutes.getStops().containsKey(truckId)) {
        existingRoutes.getStops().put(truckId, stops);
      }
    });

    newRoutes.getPaths().forEach((truckId, paths) -> {
      if (!existingRoutes.getPaths().containsKey(truckId)) {
        existingRoutes.getPaths().put(truckId, paths);
      }
    });
  }

  private void postponeOrders(List<Order> orders) {
    orders.forEach(order -> {
      order.setStatus(OrderStatus.PENDING);
      // Postponer por el intervalo actual del algoritmo
      order.setDate(order.getDate().plus(simulationConfig.getCurrentAlgorithmInterval()));
      orderCalculatingStartTime.remove(order.getId());
      log.info("Order {} postponed to {}", order.getId(), order.getDate());
    });
  }

  private void requestPlanification() {

    // Solo request planification if there are orders being calculated
    boolean hasCalculatingOrders = orderRepository.stream()
        .anyMatch(order -> order.getStatus() == OrderStatus.CALCULATING);

    if (hasCalculatingOrders) {
      lastPlanificationStart = simulatedTime;
      totalPlanificationRequests++;

      paused.set(true);
      //Fit
      orderRepository.forEach(o -> {
        boolean delayed = o.getMaxDeliveryDate().isBefore(simulatedTime.plus(Duration.ofHours(1)));
        if (delayed) {
          Duration timeBetwenCreationANdCurrent = Duration.between(o.getDate(), simulatedTime);
          o.setDeliveryLimit(timeBetwenCreationANdCurrent.plus(Duration.ofHours(4)));
        }
      });
      eventPublisher.publishEvent(
          new PlanificationRequestEvent(sessionId, plgNetwork, simulatedTime, Duration.ofSeconds(4),
              new ArrayList<>()));

    } else {
      log.trace("No orders to calculate, skipping planification request");
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
      // Reset stop indices cuando se reciben nuevas rutas
      truckCurrentStopIndex.clear();

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
        }
      }
    }

    // Only change status for CALCULATING orders to IN_PROGRESS
    List<Order> calculatingOrders = orderRepository.stream()
        .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
        .toList();

    calculatingOrders.forEach(order -> {
      order.setStatus(OrderStatus.IN_PROGRESS);
    });
    paused.set(false);
    lock.lock();
    try {
        condition.signalAll();
    } finally {
        lock.unlock();
    }
    log.info("Received updated routes from Planificator");
    try {
        SimulationMetrics metrics = calculateMetrics();
        PlanificationStatus planificationStatus = planificationService.getPlanificationStatus(sessionId);
        
        simulationNotifier.notifySnapshot(
            new SimulationSnapshot(LocalDateTime.now(), simulatedTime, plgNetwork, activeRoutes, null,
                metrics, planificationStatus));
        
        log.info("Immediate snapshot sent after planification result");
    } catch (Exception e) {
        log.error("Error sending immediate snapshot after planification: {}", e.getMessage(), e);
    }
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

  private void logDetailedState(String phase) {
    int calculating = (int) orderRepository.stream().filter(o -> o.getStatus() == OrderStatus.CALCULATING).count();
    int pending = (int) orderRepository.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
    int inProgress = (int) orderRepository.stream().filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS).count();
    int completed = (int) orderRepository.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count();
    int blocked = (int) orderRepository.stream().filter(o -> o.getStatus() == OrderStatus.BLOCKED).count();

    log.debug("STATE[{}] Time: {}, Orders[C:{}, P:{}, IP:{}, COMP:{}, BLOCKED:{}], Trucks[Active:{}], Routes:{}",
        phase, simulatedTime, calculating, pending, inProgress, completed, blocked,
        countActiveTrucks(), activeRoutes != null ? activeRoutes.getStops().size() : 0);
  }

  private void logOrderStatistics() {
    Map<OrderStatus, Long> statusCount = orderRepository.stream()
        .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

    log.warn("CRITICAL_ORDER_STATS: {}", statusCount);

    // Log órdenes que llevan mucho tiempo en CALCULATING
    orderRepository.stream()
        .filter(o -> o.getStatus() == OrderStatus.CALCULATING)
        .forEach(order -> {
          LocalDateTime startTime = orderCalculatingStartTime.get(order.getId());
          if (startTime != null) {
            Duration timeDiff = Duration.between(startTime, LocalDateTime.now());
            if (timeDiff.compareTo(Duration.ofSeconds(30)) > 0) {
              log.warn("STUCK_ORDER: {} calculating for {} seconds", order.getId(), timeDiff.getSeconds());
            }
          }
        });
  }

  private void logTruckStates(String phase) {
    plgNetwork.getTrucks().forEach(truck -> {
      log.warn("TRUCK[{}] {} - Status: {}, Loc: ({}, {}), Fuel: {}/{}, Capacity: {}/{}, Route: {}",
          phase, truck.getCode(), truck.getStatus(),
          truck.getLocation().x(), truck.getLocation().y(),
          truck.getCurrentFuel(), truck.getFuelCapacity(),
          truck.getCurrentCapacity(), truck.getMaxCapacity(),
          activeRoutes != null && activeRoutes.getStops().containsKey(truck.getId()));
    });
  }

  private void logSystemStateOnError() {
    log.error("=== SYSTEM STATE ON ERROR ===");
    log.error("Simulated Time: {}", simulatedTime);
    log.error("Next Planning: {}", nextPlanningTime);
    log.error("Running: {}, Paused: {}", running.get(), paused.get());

    logOrderStatistics();
    logTruckStates("ERROR");

    if (activeRoutes != null) {
      log.error("Active Routes: {} trucks with routes", activeRoutes.getStops().size());
      activeRoutes.getStops().forEach((truckId, stops) -> {
        log.error("  Truck {}: {} stops", truckId, stops.size());
      });
    } else {
      log.error("Active Routes: NULL");
    }

    log.error("Consecutive Failures: {}", consecutiveFailures);
    log.error("Order Calculating Times: {}", orderCalculatingStartTime.size());
  }

  private int countActiveTrucks() {
    return (int) plgNetwork.getTrucks().stream()
        .filter(t -> t.getStatus() != TruckState.BROKEN_DOWN && t.getStatus() != TruckState.MAINTENANCE)
        .count();
  }

  private void recoverFromError() {
    log.warn("Attempting error recovery at {}", simulatedTime);

    // Reset stuck orders
    handleCalculatingOrdersOverflow();

    // Reset planification state if needed
    if (lastPlanificationStart != null) {
      Duration timeSinceLastPlan = Duration.between(lastPlanificationStart, LocalDateTime.now());
      if (timeSinceLastPlan.compareTo(Duration.ofMinutes(2)) > 0) {
        log.warn("Resetting stale planification request");
        lastPlanificationStart = null;
      }
    }

    // Reduce system load
    simulationConfig.setTimeAcceleration(Math.max(1.0, simulationConfig.getTimeAcceleration() / 2));
    log.warn("Reduced acceleration to {}", simulationConfig.getTimeAcceleration());
  }
}
