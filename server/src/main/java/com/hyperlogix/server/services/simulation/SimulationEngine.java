package com.hyperlogix.server.services.simulation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

  // Order arrival rate tracking
  private final Map<LocalDateTime, Integer> orderArrivalHistory = new ConcurrentHashMap<>();
  private LocalDateTime lastOrderRateCheck = null;
  private static final Duration ORDER_RATE_CHECK_WINDOW = Duration.ofMinutes(10);

  private Set<String> trucksInMaintenance = new HashSet<>();

  // Order accumulation control fields
  private static final int MAX_CALCULATING_ORDERS = 20; // Límite máximo de órdenes en cálculo
  private static final int MAX_PENDING_ORDERS = 50; // Límite máximo de órdenes pendientes
  private static final Duration ORDER_PROCESSING_TIMEOUT = Duration.ofSeconds(80);
  private final Map<String, LocalDateTime> orderCalculatingStartTime = new ConcurrentHashMap<>();
  private final Map<String, Integer> orderRetryCount = new ConcurrentHashMap<>();
  private int consecutiveFailures = 0;
  private LocalDateTime lastSuccessfulPlanification = null;

  // Campos para monitoreo de timeouts de Ant
  private static final Duration ANT_TIMEOUT_WARNING = Duration.ofSeconds(20);
  private static final Duration ANT_TIMEOUT_CRITICAL = Duration.ofSeconds(25);
  private int antTimeoutCount = 0;
  private LocalDateTime lastAntTimeout = null;

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
    
    // Setup MDC for this simulation thread
    MDC.put("sessionId", sessionId);
    MDC.put("simulationType", "standard");
    
    log.info("=== SIMULATION START === Config K={}, Sa={}, Sc={}, Ta={}",
        simulationConfig.getTimeAcceleration(),
        simulationConfig.getCurrentAlgorithmInterval(),
        simulationConfig.getConsumptionInterval(),
        simulationConfig.getAlgorithmTime());

    if (orderRepository.isEmpty()) {
        running.set(false);
        if (onComplete != null) {
            onComplete.run();
        }
        return;
    }

    simulatedTime = orderRepository.getFirst().getDate().plus(Duration.ofNanos(1));
    nextPlanningTime = simulatedTime;
    lastOrderRateCheck = simulatedTime;

    log.info("Initial simulation time: {}, Next planning: {}", simulatedTime, nextPlanningTime);

    while (running.get()) {
      try {
        logDetailedState("LOOP_START");
        
        waitIfPaused();

        if (!running.get()) {
          log.info("Simulation stopped by external request");
          break;
        }

        // Check if all orders are completed
        if (areAllOrdersCompleted()) {
          log.info("All orders completed, ending simulation at {}", simulatedTime);
          break;
        }

        Duration timeStep = simulationConfig.getSimulationResolution()
            .multipliedBy((long) simulationConfig.getTimeAcceleration());

        simulatedTime = simulatedTime.plus(timeStep);

        // Monitor crítico específico para el día 3 de enero 2025 entre 4:30 y 5:00 AM
        if (simulatedTime.getYear() == 2025 && simulatedTime.getMonthValue() == 1 && 
            simulatedTime.getDayOfMonth() == 3 && simulatedTime.getHour() >= 4 && simulatedTime.getMinute() >= 30) {
          
          log.error("=== CRITICAL TIME MONITORING === Day 3, {}:{}, Orders in system: {}", 
                   simulatedTime.getHour(), String.format("%02d", simulatedTime.getMinute()),
                   orderRepository.size());
          
          // Log memoria y recursos del sistema
          Runtime runtime = Runtime.getRuntime();
          long usedMemory = runtime.totalMemory() - runtime.freeMemory();
          log.error("=== SYSTEM RESOURCES === Used Memory: {}MB, Active Routes: {}", 
                   usedMemory / 1024 / 1024, 
                   activeRoutes != null ? activeRoutes.getStops().size() : 0);
          
          // Snapshot de estado pre-falla a las 4:50
          if (simulatedTime.getHour() == 4 && simulatedTime.getMinute() >= 50) {
            log.error("=== PRE-FAILURE SNAPSHOT === Time: {}", simulatedTime);
            logDetailedState("PRE_FAILURE_" + simulatedTime.getMinute());
            
            // Estado detallado de camiones
            plgNetwork.getTrucks().forEach(truck -> 
                log.error("Truck {}: Status={}, Location=({},{}), Fuel={}/{}, Capacity={}/{}", 
                          truck.getId(), truck.getStatus(), truck.getLocation().x(), truck.getLocation().y(),
                          truck.getCurrentFuel(), truck.getFuelCapacity(),
                          truck.getCurrentCapacity(), truck.getMaxCapacity()));
          }
          
          // Debug específico para el pedido crítico a las 4:14 (22m3)
          if (simulatedTime.getHour() == 4 && simulatedTime.getMinute() >= 10 && simulatedTime.getMinute() <= 20) {
            log.error("=== CRITICAL ORDER PERIOD === Time: {}, Looking for large orders (>20m3)", simulatedTime);
            orderRepository.stream()
                .filter(order -> order.getRequestedGLP() >= 20)
                .forEach(order -> log.error("  Large order: {} {}m3 status={} at ({},{})", 
                                          order.getId(), order.getRequestedGLP(), order.getStatus(),
                                          order.getLocation().x(), order.getLocation().y()));
          }
        }

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

        if (simulatedTime.isAfter(nextPlanningTime)) {
          logDetailedState("BEFORE_PLANNING");
          
          // Verificar y desbloquear automáticamente órdenes cuyo tiempo de bloqueo ha terminado
          checkBlockedOrdersForUnblocking(simulatedTime);
          
          // Verificar y desbloquear órdenes que ya no están afectadas por roadblocks
          checkAndUnblockOrders(simulatedTime);
          
          int newOrderCount = getOrderBatch(simulatedTime);

          // Track order arrivals for rate calculation
          if (newOrderCount > 0) {
            orderArrivalHistory.put(simulatedTime, newOrderCount);
            log.info("Added {} new orders at {}", newOrderCount, simulatedTime);
          }

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
          recoverFromError();
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
        
        // Si hay acumulación de órdenes, aplicar medidas adicionales
        int calculatingCount = (int) orderRepository.stream()
            .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
            .count();
        
        if (calculatingCount > MAX_CALCULATING_ORDERS / 2) {
          log.warn("High order accumulation detected during deceleration, applying emergency measures");
          handleCalculatingOrdersOverflow();
        }
      }
      case "ACCELERATE" -> {
        double newAcceleration = simulationConfig.getTimeAcceleration() * 2;
        simulationConfig.setTimeAcceleration(newAcceleration);
      }
      case "EMERGENCY_CLEAR" -> {
        // Nuevo comando para limpiar acumulación manualmente
        log.warn("Emergency clear command received");
        handleCalculatingOrdersOverflow();
        
        // Resetear todos los contadores
        orderCalculatingStartTime.clear();
        orderRetryCount.clear();
        consecutiveFailures = 0;
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

        // Update truck location during travel - truck should be traveling TO the
        // current target stop
        // Only update if truck hasn't arrived yet and there's a previous stop to travel
        // from
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
   * Verifica si una orden está bloqueada por roadblocks activos durante su tiempo de disponibilidad
   */
  private boolean isOrderBlocked(Order order, LocalDateTime currentDateTime) {
    if (plgNetwork.getRoadblocks() == null || plgNetwork.getRoadblocks().isEmpty()) {
      return false;
    }
    
    LocalDateTime orderStartTime = order.getDate();
    LocalDateTime orderEndTime = order.getMaxDeliveryDate();
    
    for (Roadblock roadblock : plgNetwork.getRoadblocks()) {
      // Verificar si el roadblock está activo durante el tiempo de disponibilidad de la orden
      if (isRoadblockActiveInTimeRange(roadblock, orderStartTime, orderEndTime)) {
        // Verificar si la orden intersecta geográficamente con el roadblock
        if (locationIntersectsWithRoadblock(order.getLocation(), roadblock)) {
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
    Roadblock rdblock =  null;
    
    for (Roadblock roadblock : plgNetwork.getRoadblocks()) {
      // Verificar si el roadblock está activo durante el tiempo de disponibilidad de la orden
      if (isRoadblockActiveInTimeRange(roadblock, orderStartTime, orderEndTime)) {
        // Verificar si la orden intersecta geográficamente con el roadblock
        if (locationIntersectsWithRoadblock(order.getLocation(), roadblock)) {
          // Encontrar el tiempo de fin más temprano entre todos los roadblocks que bloquean esta orden
          if (earliestBlockEnd == null || roadblock.end().isBefore(earliestBlockEnd)) {
            earliestBlockEnd = roadblock.end();
            rdblock = roadblock;
          }
        }
      }
    }
    
    if (earliestBlockEnd != null) {
      if(order.getMaxDeliveryDate().isBefore(earliestBlockEnd)) {
        // Buscar coordenadas adyacentes disponibles
        Point originalLocation = order.getLocation();
        Point[] adjacentPoints = {
          new Point(originalLocation.x() - 1, originalLocation.y()),
          new Point(originalLocation.x() + 1, originalLocation.y()),
          new Point(originalLocation.x(), originalLocation.y() - 1),
          new Point(originalLocation.x(), originalLocation.y() + 1)
        };
        
        for (Point adjacent : adjacentPoints) {
          if (!locationIntersectsWithRoadblock(adjacent, rdblock)) {
            order.setLocation(adjacent);
            return;
          }
        }

        return;
      }
      order.setStatus(OrderStatus.BLOCKED);
      order.setBlockEndTime(earliestBlockEnd);
      log.info("Order {} set to BLOCKED due to active roadblocks, will be unblocked at {}", 
               order.getId(), earliestBlockEnd);
    }
  }
  
  /**
   * Verifica si un roadblock está activo durante un rango de tiempo dado
   * Solo considera el roadblock activo si ya ha comenzado en el tiempo actual de simulación
   */
  private boolean isRoadblockActiveInTimeRange(Roadblock roadblock, LocalDateTime startTime, LocalDateTime endTime) {
    // El roadblock debe haber comenzado ya (startTime del roadblock <= tiempo actual de simulación)
    // y debe tener superposición temporal con el período de disponibilidad de la orden
    boolean roadblockHasStarted = !roadblock.start().isAfter(simulatedTime);
    boolean hasTemporalOverlap = !(roadblock.end().isBefore(startTime) || roadblock.start().isAfter(endTime));
    
    return roadblockHasStarted && hasTemporalOverlap;
  }
  
  /**
   * Verifica si una orden intersecta geográficamente con los segmentos bloqueados de un roadblock
   */
  private boolean locationIntersectsWithRoadblock(Point orderLocation, Roadblock roadblock) {
    Set<Edge> blockedEdges = roadblock.parseRoadlock();
    
    // Distancia mínima para considerar que una orden está afectada por un bloqueo (en km)
    double INTERSECTION_THRESHOLD = 0.5; // 500 metros
    
    for (Edge blockedEdge : blockedEdges) {
      double distance = calculateDistancePointToSegment(orderLocation, blockedEdge);
      if (distance <= INTERSECTION_THRESHOLD) 
        return true;    
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
        order.setBlockEndTime(null); // Limpiar el tiempo de fin de bloqueo
        log.info("Order {} unblocked - no longer affected by roadblocks", order.getId());
      }
    }
  }
  
  /**
   * Log detallado de estados de órdenes para debugging crítico
   */
  private void logDetailedOrderStates() {
    Map<OrderStatus, List<Order>> ordersByStatus = orderRepository.stream()
        .collect(Collectors.groupingBy(Order::getStatus));
    
    ordersByStatus.forEach((status, orders) -> {
      log.error("=== ORDER STATUS: {} === Count: {}", status, orders.size());
      if (status == OrderStatus.CALCULATING) {
        orders.forEach(order -> {
          LocalDateTime startTime = orderCalculatingStartTime.get(order.getId());
          Duration processingTime = startTime != null ? 
              Duration.between(startTime, LocalDateTime.now()) : Duration.ZERO;
          log.error("  Calculating order: {} for {}s, Date: {}, Location: ({},{})", 
                   order.getId(), processingTime.getSeconds(), order.getDate(),
                   order.getLocation().x(), order.getLocation().y());
        });
      }
      if (status == OrderStatus.BLOCKED && orders.size() > 0) {
        orders.forEach(order -> {
          log.error("  Blocked order: {} until {}, Date: {}, Location: ({},{})", 
                   order.getId(), order.getBlockEndTime(), order.getDate(),
                   order.getLocation().x(), order.getLocation().y());
        });
      }
      if (status == OrderStatus.IN_PROGRESS && orders.size() > 0) {
        orders.forEach(order -> {
          log.error("  In-Progress order: {} Date: {}, Location: ({},{}), Delivered: {}/{}m3", 
                   order.getId(), order.getDate(),
                   order.getLocation().x(), order.getLocation().y(),
                   order.getDeliveredGLP(), order.getRequestedGLP());
        });
      }
    });
    
    // Resumen crítico del sistema
    log.error("=== SYSTEM SUMMARY === Time: {}", simulatedTime != null ? simulatedTime : "UNKNOWN");
    log.error("Total Orders: {}, Active Routes: {}, Ant Timeouts: {}", 
             orderRepository.size(), 
             activeRoutes != null ? activeRoutes.getStops().size() : 0,
             antTimeoutCount);
  }
  
  /**
   * Revisa órdenes bloqueadas y las desbloquea automáticamente cuando el tiempo de bloqueo ha terminado
   */
  private void checkBlockedOrdersForUnblocking(LocalDateTime currentDateTime) {
    List<Order> blockedOrders = orderRepository.stream()
        .filter(order -> order.getStatus() == OrderStatus.BLOCKED)
        .filter(order -> order.getBlockEndTime() != null)
        .toList();
    
    for (Order order : blockedOrders) {
      // Si el tiempo actual es mayor o igual al tiempo de fin del bloqueo, desbloquear la orden
      if (!currentDateTime.isBefore(order.getBlockEndTime())) {
        order.setStatus(OrderStatus.CALCULATING);
        order.setBlockEndTime(null); // Limpiar el tiempo de fin de bloqueo
        orderCalculatingStartTime.put(order.getId(), LocalDateTime.now());
        log.info("Order {} automatically unblocked at {} - moved to CALCULATING", 
                 order.getId(), currentDateTime);
      }
    }
  }

  private int getOrderBatch(LocalDateTime currentDateTime) {
    // Debug crítico para el día 3 de enero 2025 a las 4:30 AM
    if (currentDateTime.getYear() == 2025 && currentDateTime.getMonthValue() == 1 && 
        currentDateTime.getDayOfMonth() == 3 && currentDateTime.getHour() >= 4 && currentDateTime.getMinute() >= 30) {
      log.error("=== CRITICAL DEBUG DAY 3 04:30+ === Time: {}", currentDateTime);
      log.error("=== ORDER BATCH DEBUG === Pending: {}, Calculating: {}, InProgress: {}, Blocked: {}", 
               orderRepository.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count(),
               orderRepository.stream().filter(o -> o.getStatus() == OrderStatus.CALCULATING).count(),
               orderRepository.stream().filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS).count(),
               orderRepository.stream().filter(o -> o.getStatus() == OrderStatus.BLOCKED).count());
      
      log.error("=== NETWORK STATE === Trucks: {}, Available: {}, Maintenance: {}, BrokenDown: {}", 
               plgNetwork.getTrucks().size(),
               plgNetwork.getTrucks().stream().filter(t -> t.getStatus() == TruckState.IDLE || t.getStatus() == TruckState.ACTIVE).count(),
               plgNetwork.getTrucks().stream().filter(t -> t.getStatus() == TruckState.MAINTENANCE).count(),
               plgNetwork.getTrucks().stream().filter(t -> t.getStatus() == TruckState.BROKEN_DOWN).count());
      
      // NUEVO: Log cada orden individual y su estado
      log.error("=== DETAILED ORDER ANALYSIS === Time: {}", currentDateTime);
      orderRepository.forEach(order -> {
        log.error("Order {}: Status={}, Date={}, Location=({},{}), GLP={}/{}m3, Client={}", 
                 order.getId(), order.getStatus(), order.getDate(),
                 order.getLocation().x(), order.getLocation().y(),
                 order.getDeliveredGLP(), order.getRequestedGLP(), order.getClientId());
      });
      
      // NUEVO: Verificar si hay roadblocks activos
      if (plgNetwork.getRoadblocks() != null && !plgNetwork.getRoadblocks().isEmpty()) {
        log.error("=== ACTIVE ROADBLOCKS === Count: {}", plgNetwork.getRoadblocks().size());
        plgNetwork.getRoadblocks().forEach(roadblock -> {
          log.error("Roadblock: {} to {} ({})", roadblock.start(), roadblock.end(), 
                   roadblock.parseRoadlock().size() + " segments");
        });
      } else {
        log.error("=== NO ROADBLOCKS DETECTED ===");
      }
      
      // NUEVO: Estado detallado de cada camión
      plgNetwork.getTrucks().forEach(truck -> {
        log.error("Truck {}: Status={}, Fuel={}/{}, Capacity={}/{}, Location=({},{})", 
                 truck.getId(), truck.getStatus(), 
                 truck.getCurrentFuel(), truck.getFuelCapacity(),
                 truck.getCurrentCapacity(), truck.getMaxCapacity(),
                 truck.getLocation().x(), truck.getLocation().y());
      });
      
      logDetailedOrderStates();
    }
    
    // Verificar límites antes de procesar nuevas órdenes
    int currentCalculatingCount = (int) orderRepository.stream()
        .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
        .count();
    
    int currentPendingCount = (int) orderRepository.stream()
        .filter(order -> order.getStatus() == OrderStatus.PENDING && 
                order.getDate().isBefore(currentDateTime))
        .count();

    
    
    // Si hay demasiadas órdenes en cálculo, no agregar más
    if (currentCalculatingCount >= MAX_CALCULATING_ORDERS) {
      log.warn("Too many orders calculating ({}), skipping new batch processing", currentCalculatingCount);
      
      // Aplicar estrategia de emergencia para liberar órdenes bloqueadas
      handleCalculatingOrdersOverflow();
      return 0;
    }
    
    // Si hay demasiadas órdenes pendientes, aplicar filtros
    if (currentPendingCount >= MAX_PENDING_ORDERS) {
      log.warn("Too many pending orders ({}), applying filtering strategy", currentPendingCount);
      return applyOrderFiltering(currentDateTime);
    }
    
    // Procesar normalmente con límites
    List<Order> candidateOrders = orderRepository.stream()
        .filter(order -> order.getDate().isBefore(currentDateTime) && 
                order.getStatus() == OrderStatus.PENDING)
        .limit(MAX_CALCULATING_ORDERS - currentCalculatingCount) // Respetar límite
        .toList();

    List<Order> newOrders = new ArrayList<>();
    
    if (!candidateOrders.isEmpty()) {
      for (Order order : candidateOrders) {
        // Verificar si la orden está bloqueada por roadblocks activos
        if (isOrderBlocked(order, currentDateTime)) {
          blockOrder(order, currentDateTime);
        } else {
          order.setStatus(OrderStatus.CALCULATING);
          orderCalculatingStartTime.put(order.getId(), LocalDateTime.now());
          newOrders.add(order);
          log.info("New order {} added to batch for calculation", order.getId());
        }
      }

      // NO recalcular órdenes IN_PROGRESS automáticamente si hay muchas órdenes
      if (currentCalculatingCount + newOrders.size() < MAX_CALCULATING_ORDERS / 2) {
        List<Order> inProgressOrders = orderRepository.stream()
            .filter(order -> order.getStatus() == OrderStatus.IN_PROGRESS)
            .limit(5) // Límite pequeño para reoptimización
            .toList();

        inProgressOrders.forEach(order -> {
          order.setStatus(OrderStatus.CALCULATING);
          orderCalculatingStartTime.put(order.getId(), LocalDateTime.now());
          log.info("Order {} set back to CALCULATING due to new batch", order.getId());
        });
      }
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

  private int applyOrderFiltering(LocalDateTime currentDateTime) {
    log.info("Applying order filtering due to high pending order count");
    
    // Estrategia: solo procesar órdenes más prioritarias
    List<Order> allPendingOrders = orderRepository.stream()
        .filter(order -> order.getDate().isBefore(currentDateTime) && 
                order.getStatus() == OrderStatus.PENDING)
        .sorted((o1, o2) -> {
          // Priorizar por:
          // 1. Antigüedad de la orden
          // 2. Tamaño (órdenes más pequeñas primero)
          int ageComparison = o1.getDate().compareTo(o2.getDate());
          if (ageComparison != 0) return ageComparison;
          return Integer.compare(o1.getRequestedGLP(), o2.getRequestedGLP());
        })
        .toList();
    
    // Solo procesar las primeras N órdenes más prioritarias
    int maxToProcess = Math.max(5, MAX_CALCULATING_ORDERS / 4);
    List<Order> selectedOrders = allPendingOrders.stream()
        .limit(maxToProcess)
        .toList();
    
    selectedOrders.forEach(order -> {
      order.setStatus(OrderStatus.CALCULATING);
      orderCalculatingStartTime.put(order.getId(), LocalDateTime.now());
      log.info("High-priority order {} selected for calculation", order.getId());
    });
    
    // El resto se mantiene en PENDING para procesamiento posterior
    return selectedOrders.size();
  }

  /**
   * Aplica una estrategia de emergencia para órdenes que no pueden ser procesadas por el algoritmo de hormigas
   */
  private void applyEmergencyFallback(List<Order> orders) {
    log.warn("Applying emergency fallback for {} orders", orders.size());
    
    if (orders.isEmpty()) {
      return;
    }
    
    // Estrategia 1: Intentar con algoritmo greedy si hay pocos órdenes
    if (orders.size() <= 5) {
      processOrdersWithGreedyAlgorithm(orders);
      return;
    }
    
    // Estrategia 2: Para muchas órdenes, postponer algunas y procesar las más urgentes
    orders.sort((o1, o2) -> o1.getDate().compareTo(o2.getDate())); // Más antiguos primero
    
    int urgentCount = Math.min(3, orders.size());
    List<Order> urgentOrders = orders.subList(0, urgentCount);
    List<Order> postponedOrders = orders.subList(urgentCount, orders.size());
    
    log.info("Emergency fallback: processing {} urgent orders, postponing {} orders", 
             urgentOrders.size(), postponedOrders.size());
    
    // Procesar órdenes urgentes con algoritmo greedy
    processOrdersWithGreedyAlgorithm(urgentOrders);
    
    // Postponer el resto
    postponeOrders(postponedOrders);
  }
  
  /**
   * Restaura parámetros óptimos cuando el sistema se ha estabilizado
   */
  private void restoreOptimalParameters() {
    // Aquí se pueden restaurar parámetros del algoritmo de hormigas si se hubieran reducido
    // Por ejemplo, restaurar el número de hormigas, iteraciones, etc.
    log.info("System stabilized - optimal parameters restored");
  }
  
  /**
   * Aplica estrategia de complejidad reducida para evitar timeouts
   */
  private void applyReducedComplexityStrategy() {
    log.warn("Applying reduced complexity strategy due to timeouts");
    
    // Limitar el número de órdenes CALCULATING para reducir la carga
    List<Order> calculatingOrders = orderRepository.stream()
        .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
        .toList();
    
    if (calculatingOrders.size() > 3) {
      // Mantener solo las 3 más antiguas en CALCULATING
      calculatingOrders.sort((o1, o2) -> o1.getDate().compareTo(o2.getDate()));
      
      List<Order> keepCalculating = calculatingOrders.subList(0, 3);
      List<Order> revertToPending = calculatingOrders.subList(3, calculatingOrders.size());
      
      revertToPending.forEach(order -> {
        order.setStatus(OrderStatus.PENDING);
        log.info("Order {} reverted to PENDING due to complexity reduction", order.getId());
      });
      
      log.info("Reduced complexity: keeping {} orders in CALCULATING, reverted {} to PENDING", 
               keepCalculating.size(), revertToPending.size());
    }
  }
  
  /**
   * Crea un network altamente optimizado que envía SOLO las órdenes que el optimizador necesita procesar
   * Esta es la solución al problema crítico detectado: el algoritmo colapsa cuando recibe 98 órdenes
   * pero solo necesita procesar 2 CALCULATING
   */
  private PLGNetwork createOptimizedNetworkForPlanification() {
    // Clonar el network original
    PLGNetwork optimizedNetwork = plgNetwork.clone();
    
    // Filtrar SOLO las órdenes que el optimizador necesita procesar
    List<Order> relevantOrders = new ArrayList<>();
    
    // 1. SIEMPRE incluir órdenes CALCULATING (estas son las prioritarias)
    List<Order> calculatingOrders = orderRepository.stream()
        .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
        .map(Order::clone)
        .toList();
    relevantOrders.addAll(calculatingOrders);
    
    // 2. Incluir órdenes IN_PROGRESS solo si están realmente siendo re-optimizadas
    List<Order> reoptimizingOrders = orderRepository.stream()
        .filter(order -> order.getStatus() == OrderStatus.IN_PROGRESS)
        .filter(order -> needsReoptimization(order))
        .limit(3) // Máximo 3 para evitar sobrecarga
        .map(Order::clone)
        .toList();
    relevantOrders.addAll(reoptimizingOrders);
    
    // NO incluir órdenes COMPLETED, PENDING, o BLOCKED
    // El algoritmo NO necesita estas para procesar las órdenes CALCULATING
    
    optimizedNetwork.setOrders(relevantOrders);
    
    // Debug crítico específico para el día 3 4:50+
    if (simulatedTime.getYear() == 2025 && simulatedTime.getMonthValue() == 1 && 
        simulatedTime.getDayOfMonth() == 3 && simulatedTime.getHour() >= 4 && simulatedTime.getMinute() >= 50) {
      
      log.error("=== NETWORK OPTIMIZATION APPLIED ===");
      log.error("Original network orders: {}, Optimized network orders: {}", 
               plgNetwork.getOrders().size(), relevantOrders.size());
      log.error("Calculating orders: {}, Reoptimizing orders: {}", 
               calculatingOrders.size(), reoptimizingOrders.size());
    }
    
    log.info("NETWORK OPTIMIZATION: Reduced from {} to {} orders for optimizer (CALCULATING: {}, REOPT: {})", 
             plgNetwork.getOrders().size(), relevantOrders.size(), 
             calculatingOrders.size(), reoptimizingOrders.size());
    
    return optimizedNetwork;
  }

  /**
   * Determina si una orden IN_PROGRESS necesita reoptimización
   */
  private boolean needsReoptimization(Order order) {
    // Solo reoptimizar órdenes IN_PROGRESS que llevan mucho tiempo estancadas
    return simulatedTime.minusMinutes(30).isAfter(order.getDate());
  }

  /**
   * Calcula tiempo óptimo de timeout basado en la carga del sistema
   */
  private Duration calculateOptimalAlgorithmTime(int orderCount) {
    // Tiempo base mínimo de 10 segundos
    long baseTimeSeconds = 10;
    
    // Añadir tiempo adicional basado en número de órdenes
    long additionalTime = Math.min(orderCount * 2L, 30L); // Máximo 30 segundos adicionales
    
    // Factores adicionales si hay timeouts recientes
    if (antTimeoutCount > 0) {
      // Si hay timeouts recientes, usar tiempo más conservador
      additionalTime += antTimeoutCount * 5L;
    }
    
    Duration optimalTime = Duration.ofSeconds(baseTimeSeconds + additionalTime);
    log.debug("Calculated optimal algorithm time: {}s for {} orders (timeout count: {})", 
              optimalTime.getSeconds(), orderCount, antTimeoutCount);
    
    return optimalTime;
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
    Point fromLocation = stops.size() > 1 ? 
        stops.get(stops.size() - 2).getNode().getLocation() : 
        truck.getLocation();
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
    // Verificar si acabamos de tener un timeout de Ant
    boolean riskOfTimeout = false;
    if (lastAntTimeout != null) {
      Duration timeSinceTimeout = Duration.between(lastAntTimeout, LocalDateTime.now());
      if (timeSinceTimeout.compareTo(Duration.ofSeconds(10)) < 0) {
        log.warn("Recent Ant timeout detected, applying reduced complexity strategy");
        applyReducedComplexityStrategy();
        riskOfTimeout = true;
      }
    }
    
    // Verificar acumulación antes de solicitar planificación
    int calculatingCount = (int) orderRepository.stream()
        .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
        .count();
    
    // Si hay acumulación crítica, aplicar medidas de emergencia
    if (calculatingCount >= MAX_CALCULATING_ORDERS) {
      log.error("Critical order accumulation detected ({} orders), applying emergency processing", 
               calculatingCount);
      handleCalculatingOrdersOverflow();
      return; // No solicitar planificación normal
    }
    
    // Detectar riesgo de timeout por múltiples factores
    int inProgressCount = (int) orderRepository.stream()
        .filter(order -> order.getStatus() == OrderStatus.IN_PROGRESS)
        .count();
    
    if (antTimeoutCount > 2 || calculatingCount + inProgressCount > 8) {
      riskOfTimeout = true;
      log.warn("High timeout risk detected - timeout count: {}, total active orders: {}", 
               antTimeoutCount, calculatingCount + inProgressCount);
    }
    
    // Verificar si hay órdenes bloqueadas por mucho tiempo
    checkForStuckOrders();
    
    // Solo request planification if there are orders being calculated
    boolean hasCalculatingOrders = orderRepository.stream()
        .anyMatch(order -> order.getStatus() == OrderStatus.CALCULATING);

    if (hasCalculatingOrders) {
      lastPlanificationStart = LocalDateTime.now();
      totalPlanificationRequests++;
      
      // Ajustar tiempo de algoritmo basado en historial de timeouts
      Duration algorithmTime = calculateOptimalAlgorithmTime(calculatingCount);
      
      // Debug crítico para el día 3 de enero 2025 a las 4:50 AM
      if (simulatedTime.getYear() == 2025 && simulatedTime.getMonthValue() == 1 && 
          simulatedTime.getDayOfMonth() == 3 && simulatedTime.getHour() >= 4 && simulatedTime.getMinute() >= 50) {
        log.error("=== CRITICAL PLANIFICATION REQUEST === Time: {}", simulatedTime);
        log.error("=== PRE-OPTIMIZER NETWORK STATE ===");
        log.error("Orders in repository: {}", orderRepository.size());
        log.error("Trucks in network: {}", plgNetwork.getTrucks().size());
        log.error("Stations in network: {}", plgNetwork.getStations().size());
        log.error("Roadblocks in network: {}", plgNetwork.getRoadblocks() != null ? plgNetwork.getRoadblocks().size() : 0);
        log.error("Calculating orders count: {}", calculatingCount);
        log.error("Risk of timeout: {}", riskOfTimeout);
        log.error("Ant timeout count: {}", antTimeoutCount);
        
        orderRepository.stream()
            .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
            .forEach(order -> {
              log.error("CALCULATING Order {}: GLP={}m3, Location=({},{}), Date={}, Client={}", 
                       order.getId(), order.getRequestedGLP(), 
                       order.getLocation().x(), order.getLocation().y(),
                       order.getDate(), order.getClientId());
            });
        
        plgNetwork.getTrucks().stream()
            .filter(truck -> truck.getStatus() == TruckState.IDLE || truck.getStatus() == TruckState.ACTIVE)
            .forEach(truck -> {
              log.error("Available Truck {}: Fuel={}/{}, Capacity={}/{}, Location=({},{})", 
                       truck.getId(), truck.getCurrentFuel(), truck.getFuelCapacity(),
                       truck.getCurrentCapacity(), truck.getMaxCapacity(),
                       truck.getLocation().x(), truck.getLocation().y());
            });
      }
      
      // CAMBIO CRÍTICO: Usar network optimizado en lugar del network completo o filtrado
      PLGNetwork networkToSend = createOptimizedNetworkForPlanification();
      
      int totalOrdersInNetwork = networkToSend.getOrders().size();
      
      // Debug crítico específico para el día 3 4:50+
      if (simulatedTime.getYear() == 2025 && simulatedTime.getMonthValue() == 1 && 
          simulatedTime.getDayOfMonth() == 3 && simulatedTime.getHour() >= 4 && simulatedTime.getMinute() >= 50) {
        
        log.error("=== FINAL NETWORK TO OPTIMIZER ===");
        log.error("Network being sent - Orders: {}, Trucks: {}, Stations: {}", 
                 totalOrdersInNetwork, networkToSend.getTrucks().size(), networkToSend.getStations().size());
        
        networkToSend.getOrders().forEach(order -> {
          log.error("Network Order {}: Status={}, GLP={}m3, Location=({},{})", 
                   order.getId(), order.getStatus(), order.getRequestedGLP(),
                   order.getLocation().x(), order.getLocation().y());
        });
      }
      
      log.info("Requesting planification with {} calculating orders, {} total orders in network (optimized), algorithm time: {}, filtered: {}", 
              calculatingCount, totalOrdersInNetwork, algorithmTime, riskOfTimeout);
      
      eventPublisher.publishEvent(
          new PlanificationRequestEvent(sessionId, networkToSend, simulatedTime, algorithmTime,
              new ArrayList<>()));
    } else {
      log.trace("No orders to calculate, skipping planification request");
    }
  }

  private void checkForStuckOrders() {
    LocalDateTime now = LocalDateTime.now();
    long stuckCount = orderRepository.stream()
        .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
        .filter(order -> {
          LocalDateTime startTime = orderCalculatingStartTime.get(order.getId());
          return startTime != null && 
                 Duration.between(startTime, now).compareTo(ORDER_PROCESSING_TIMEOUT) > 0;
        })
        .count();
    
    if (stuckCount > 5) {
      log.warn("Detected {} stuck orders, triggering emergency processing", stuckCount);
      handleCalculatingOrdersOverflow();
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
    LocalDateTime now = LocalDateTime.now();
    
    // Debug crítico específico para el día 3 4:50+
    if (simulatedTime.getYear() == 2025 && simulatedTime.getMonthValue() == 1 && 
        simulatedTime.getDayOfMonth() == 3 && simulatedTime.getHour() >= 4 && simulatedTime.getMinute() >= 50) {
      
      log.error("=== CRITICAL PLANIFICATION RESULT === Time: {}", simulatedTime);
      log.error("=== OPTIMIZER RESPONSE ===");
      if (routes == null) {
        log.error("RECEIVED NULL ROUTES FROM OPTIMIZER!");
      } else {
        log.error("Received routes - Trucks with routes: {}, Total stops: {}, Total paths: {}", 
                 routes.getStops().size(), 
                 routes.getStops().values().stream().mapToInt(List::size).sum(),
                 routes.getPaths().values().stream().mapToInt(List::size).sum());
        
        // Log detalles de cada ruta recibida
        routes.getStops().forEach((truckId, stops) -> {
          log.error("Route for Truck {}: {} stops", truckId, stops.size());
          stops.forEach(stop -> {
            log.error("  Stop: {} at {} (type: {})", 
                     stop.getNode().getId(), stop.getArrivalTime(), stop.getNode().getType());
          });
        });
      }
    }
    
    // Verificar si esta respuesta viene después de un timeout
    if (lastPlanificationStart != null) {
      Duration planificationDuration = Duration.between(lastPlanificationStart, now);
      
      if (planificationDuration.compareTo(ANT_TIMEOUT_CRITICAL) > 0) {
        log.error("PLANIFICATION TIMEOUT DETECTED: Duration was {} seconds", planificationDuration.getSeconds());
        antTimeoutCount++;
        lastAntTimeout = now;
        
        if (routes == null) {
          log.error("Timeout resulted in NULL routes - applying emergency measures");
        }
      } else if (planificationDuration.compareTo(ANT_TIMEOUT_WARNING) > 0) {
        log.warn("SLOW PLANIFICATION WARNING: Duration was {} seconds", planificationDuration.getSeconds());
      }
      
      totalPlanificationTime = totalPlanificationTime.plus(planificationDuration);
      log.info("Planification completed in {} seconds (total requests: {}, avg time: {}s)", 
              planificationDuration.getSeconds(), totalPlanificationRequests,
              totalPlanificationTime.dividedBy(totalPlanificationRequests).getSeconds());
    }
    
    if (routes == null || routes.getStops().isEmpty()) {
      log.warn("Received null or empty routes, applying fallback strategy");
      consecutiveFailures++;
      
      if (antTimeoutCount > 0) {
        log.error("NULL routes after {} recent timeouts - system may be overloaded", antTimeoutCount);
      }
      
      List<Order> calculatingOrders = orderRepository.stream()
          .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
          .toList();
      
      // En lugar de solo cambiar a PENDING, aplicar estrategia de fallback
      applyEmergencyFallback(calculatingOrders);
      return;
    }
    
    // Planificación exitosa - resetear contadores de timeout si no hubo problemas recientes
    if (antTimeoutCount > 0 && (lastAntTimeout == null || 
        Duration.between(lastAntTimeout, now).compareTo(Duration.ofMinutes(2)) > 0)) {
      log.info("Successful planification after timeouts, resetting timeout counter from {}", antTimeoutCount);
      antTimeoutCount = Math.max(0, antTimeoutCount - 1); // Reducir gradualmente
    }
    
    consecutiveFailures = 0;
    lastSuccessfulPlanification = now;
    
    // Limpiar timers de planificación exitosa
    orderRepository.stream()
        .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
        .forEach(order -> {
          orderCalculatingStartTime.remove(order.getId());
          orderRetryCount.remove(order.getId());
        });
    
    // Restaurar parámetros si la situación se ha estabilizado
    if (antTimeoutCount == 0) {
      restoreOptimalParameters();
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
