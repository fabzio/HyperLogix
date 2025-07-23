package com.hyperlogix.server.services.simulation;

import com.hyperlogix.server.config.Constants;
import com.hyperlogix.server.domain.*;
import com.hyperlogix.server.features.operation.repository.RealTimeOrderRepository;
import com.hyperlogix.server.features.planification.dtos.PlanificationRequestEvent;
import com.hyperlogix.server.services.incident.IncidentManagement;
import com.hyperlogix.server.services.planification.PlanificationService;
import com.hyperlogix.server.services.planification.PlanificationStatus;

import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class RealTimeSimulationEngine implements Runnable {
  // Incident management for real-time simulation
  private List<Incident> incidentRepository = new java.util.ArrayList<>();
  private com.hyperlogix.server.services.incident.IncidentManagement incidentManager = null;
  private static final Logger log = LoggerFactory.getLogger(RealTimeSimulationEngine.class);

  private final ApplicationEventPublisher eventPublisher;
  private final PlanificationService planificationService;
  private final BlockadeProcessor blockadeProcessor;
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

  // Blockade tracking
  private List<Roadblock> lastActiveBlockades = List.of();
  private LocalDateTime lastBlockadeCheck = null;
  private static final Duration BLOCKADE_CHECK_INTERVAL = Duration.ofMinutes(5);

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

  // Truck maintenance tracking
  private Set<String> trucksInMaintenance = new HashSet<>();

  public RealTimeSimulationEngine(String sessionId,
      SimulationConfig simulationConfig,
      SimulationNotifier simulationNotifier,
      RealTimeOrderRepository realTimeOrderRepository,
      ApplicationEventPublisher eventPublisher,
      PlanificationService planificationService,
      BlockadeProcessor blockadeProcessor) {
    this.sessionId = sessionId;
    this.simulationConfig = simulationConfig;
    this.simulationNotifier = simulationNotifier;
    this.realTimeOrderRepository = realTimeOrderRepository;
    this.eventPublisher = eventPublisher;
    this.planificationService = planificationService;
    this.blockadeProcessor = blockadeProcessor;
    // Initialize incident manager if PLGNetwork is available
    if (plgNetwork != null) {
      incidentManager = new IncidentManagement(incidentRepository);
    }
  }

  public void handleIncidentReport(String truckCode, IncidentType incidentType, String turn) {
    Incident incident = new Incident();
    incident.setId(java.util.UUID.randomUUID().toString());
    incident.setTruckCode(truckCode);
    incident.setType(incidentType);
    incident.setTurn(turn);
    incident.setDaysSinceIncident(0);
    incident.setStatus(IncidentStatus.IMMOBILIZED);
    incident.setIncidentTime(simulatedTime);
    incident.setFuel(plgNetwork.getTrucks().stream()
        .filter(t -> t.getCode().equals(truckCode))
        .findFirst()
        .map(t -> (int) t.getCurrentFuel())
        .orElse(0));
    incidentRepository.add(incident);
    if (incidentManager == null) {
      incidentManager = new IncidentManagement(incidentRepository);
    }
    // Find the truck in the network
    if (plgNetwork != null) {
      Truck truck = plgNetwork.getTrucks().stream()
          .filter(t -> t.getCode().equals(truckCode))
          .findFirst().orElse(null);
      if (truck != null) {
        // Process the incident for the truck
        incidentManager.handleIncidentWithManagement(truck, incident, simulatedTime);
        log.info("Incident {} applied to truck {} in real-time", incident.getType(), truck.getCode());
      } else {
        log.warn("Truck with code {} not found in PLGNetwork for incident", truckCode);
      }
    }
    realTimeOrderRepository.getAllOrders().stream()
        .forEach(o -> o.setStatus(OrderStatus.PENDING));
    plgNetwork.setIncidents(incidentRepository);
    triggerImmediateUpdate();
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

      // Check for scheduled truck maintenances
      boolean needsReplanning = checkScheduledMaintenances();

      if (needsReplanning) {
        log.info("Maintenance changes detected, requesting immediate replanning");
        forceReplanification = true;
        requestPlanification();
      }

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

        // Check for dynamic blockade changes
        boolean blockadeChanged = checkForBlockadeChanges();

        // Request planification only if orders have changed, there are new orders to
        // process, or blockades have changed
        if (ordersToProcess > 0 && hasOrdersChanged()) {
          requestPlanification();
        } else if (blockadeChanged) {
          log.info("Blockade changes detected, requesting replanification");
          forceReplanification = true;
          requestPlanification();
        } else if (ordersToProcess > 0) {
          log.debug("Orders to process ({}) but no changes detected, skipping planification", ordersToProcess);
        } else {
          // No orders to process - check if we should clear active routes
          checkAndClearRoutesIfNoActiveOrders();
        }

        nextPlanningTime = nextPlanningTime
            .plus(simulationConfig.getConsumptionInterval());
        log.info("Next planning time: {} (interval: {})", nextPlanningTime,
            simulationConfig.getCurrentAlgorithmInterval());
      }
      var truckProgress = updateSystemState(timeStep);

      // Calculate metrics and notify with snapshot
      SimulationMetrics metrics = calculateMetrics();
      PlanificationStatus planificationStatus = planificationService.getPlanificationStatus(sessionId);

      // Update PLG network with current orders from repository
      PLGNetwork updatedNetwork = updatePLGNetworkWithCurrentOrders();

      simulationNotifier
          .notifySnapshot(
              new SimulationSnapshot(LocalDateTime.now(), simulatedTime, updatedNetwork, activeRoutes, truckProgress,
                  metrics,
                  planificationStatus));

      // Calculate sleep duration based on acceleration - higher acceleration means
      // shorter sleep
      Duration sleepDuration = Duration.ofMillis(
          (long) (simulationConfig.getSimulationResolution().toMillis() / simulationConfig.getTimeAcceleration()));

      log.debug("Simulation loop: acceleration={}, timeStep={}ms, sleepDuration={}ms",
          simulationConfig.getTimeAcceleration(), timeStep.toMillis(), sleepDuration.toMillis());

      sleep(sleepDuration);
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

      // When there are new orders, reconsider ALL IN_PROGRESS orders for
      // optimization to ensure they can be re-routed if needed
      List<Order> inProgressOrders = realTimeOrderRepository.getAllOrders().stream()
          .filter(order -> order.getStatus() == OrderStatus.IN_PROGRESS)
          .toList();

      if (!inProgressOrders.isEmpty()) {
        log.info("Reconsidering {} IN_PROGRESS orders for optimization due to {} new orders",
            inProgressOrders.size(), newOrders.size());

        inProgressOrders.forEach(order -> {
          realTimeOrderRepository.updateOrderStatus(order.getId(), OrderStatus.CALCULATING);
          log.info("Order {} (IN_PROGRESS -> CALCULATING) will be reconsidered for re-routing", order.getId());
        });

        totalOrdersToProcess += inProgressOrders.size();
        log.info("Total orders for planification: {} new + {} reconsidered = {}",
            newOrders.size(), inProgressOrders.size(), totalOrdersToProcess);
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
        TruckState oldState = truck.getStatus();
        truck.setStatus(newState);

        // Handle maintenance state transitions
        if (newState == TruckState.MAINTENANCE && oldState != TruckState.MAINTENANCE) {
          // Starting maintenance - set nextMaintenance to current simulation time
          truck.setNextMaintenance(simulatedTime);
          trucksInMaintenance.add(truck.getId());
          log.info("Truck {} entered maintenance at simulation time {}", truck.getCode(), simulatedTime);
        } else if (oldState == TruckState.MAINTENANCE && newState != TruckState.MAINTENANCE) {
          // Ending maintenance - schedule next maintenance
          truck.endMaintenance(simulatedTime);
          trucksInMaintenance.remove(truck.getId());
          log.info("Truck {} ended maintenance at simulation time {}, next maintenance: {}",
              truck.getCode(), simulatedTime, truck.getNextMaintenance());
        } else if (newState == TruckState.IDLE && oldState == TruckState.BROKEN_DOWN) {
          incidentRepository.remove(
              incidentRepository.stream()
                  .filter(i -> i.getTruckCode().equals(truck.getCode()) && i.getStatus() == IncidentStatus.IMMOBILIZED)
                  .findFirst().orElse(null));
        }
      } else {
        log.warn("Truck with ID {} not found when trying to update state to {}", truckId, newState);
      }
      triggerImmediatePlanification();
    }
  }

  /**
   * Checks for scheduled truck maintenances and handles them automatically.
   * Similar to SimulationEngine but adapted for real-time operation.
   * 
   * @return true if any maintenance changes require replanning
   */
  private boolean checkScheduledMaintenances() {
    if (plgNetwork == null) {
      return false;
    }

    boolean needsReplanning = false;
    for (Truck truck : plgNetwork.getTrucks()) {
      if (truck.getNextMaintenance() != null) {
        // If maintenance time has arrived and truck is not already in maintenance
        if (activeRoutes != null
            && (simulatedTime.isEqual(truck.getNextMaintenance()) || simulatedTime.isAfter(truck.getNextMaintenance()))
            && truck.getStatus() != TruckState.MAINTENANCE) {
          boolean wasInRoute = startTruckMaintenance(truck);
          if (wasInRoute) {
            needsReplanning = true;
          }
        }

        // If maintenance period has ended (after 24 hours), end maintenance
        if (simulatedTime.isAfter(truck.getNextMaintenance().plusHours(24))
            && truck.getStatus() == TruckState.MAINTENANCE) {
          endTruckMaintenance(truck);
          needsReplanning = true;
        }
      }
    }

    return needsReplanning;
  }

  /**
   * Starts maintenance for a truck, interrupting current route if necessary.
   * 
   * @param truck The truck to start maintenance for
   * @return true if the truck was in route and needs replanning
   */
  private boolean startTruckMaintenance(Truck truck) {
    log.info("Starting scheduled maintenance for truck {} at {}", truck.getCode(), simulatedTime);

    // Check if truck is currently in route
    boolean wasInRoute = isInRoute(truck);

    if (wasInRoute) {
      log.warn("Truck {} is in route, interrupting current route for scheduled maintenance", truck.getCode());

      // Interrupt current route immediately
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

  /**
   * Ends maintenance for a truck and schedules next maintenance.
   * 
   * @param truck The truck to end maintenance for
   */
  private void endTruckMaintenance(Truck truck) {
    log.info("Ending scheduled maintenance for truck {} at {}", truck.getCode(), simulatedTime);

    // Change status to active and schedule next maintenance
    truck.endMaintenance(simulatedTime);
    trucksInMaintenance.remove(truck.getId());
  }

  /**
   * Checks if a truck is currently in route.
   * 
   * @param truck The truck to check
   * @return true if truck has active stops assigned
   */
  private boolean isInRoute(Truck truck) {
    if (activeRoutes == null) {
      return false;
    }
    List<Stop> stops = activeRoutes.getStops().getOrDefault(truck.getId(), List.of());
    return stops.size() > 1;
  }

  /**
   * Schedules a future maintenance for a truck.
   * 
   * @param truckId         The ID of the truck to schedule maintenance for
   * @param maintenanceTime The time when maintenance should occur
   */
  public void scheduleTruckMaintenance(String truckId, LocalDateTime maintenanceTime) {
    if (plgNetwork != null) {
      Truck truck = plgNetwork.getTrucks().stream()
          .filter(t -> t.getId().equals(truckId))
          .findFirst()
          .orElse(null);

      if (truck != null) {
        truck.setNextMaintenance(maintenanceTime);
        log.info("Scheduled maintenance for truck {} at {}", truck.getCode(), maintenanceTime);
      } else {
        log.warn("Truck with ID {} not found when trying to schedule maintenance", truckId);
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
          new SimulationSnapshot(LocalDateTime.now(), simulatedTime, updatedNetwork, activeRoutes,null, metrics,
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
    log.info("Real-time simulation {} received command: {}", sessionId, command);
    switch (command.toUpperCase()) {
      case "PAUSE" -> {
        log.info("Real-time simulation {} executing PAUSE command", sessionId);
        paused.set(true);
        log.info("Real-time simulation {} paused successfully", sessionId);
      }
      case "RESUME" -> {
        log.info("Real-time simulation {} executing RESUME command", sessionId);
        paused.set(false);
        lock.lock();
        try {
          condition.signalAll();
        } finally {
          lock.unlock();
        }
        log.info("Real-time simulation {} resumed successfully", sessionId);
      }
      case "DESACCELERATE" -> {
        double currentAcceleration = simulationConfig.getTimeAcceleration();
        double newAcceleration = Math.max(0.5, currentAcceleration / 2);
        log.info("Real-time simulation {} executing DESACCELERATE command: {} -> {}", sessionId, currentAcceleration,
            newAcceleration);
        simulationConfig.setTimeAcceleration(newAcceleration);
        log.info("Real-time simulation {} decelerated to {}x", sessionId, newAcceleration);
      }
      case "ACCELERATE" -> {
        double currentAcceleration = simulationConfig.getTimeAcceleration();
        double newAcceleration = Math.min(32.0, currentAcceleration * 2);
        log.info("Real-time simulation {} executing ACCELERATE command: {} -> {}", sessionId, currentAcceleration,
            newAcceleration);
        simulationConfig.setTimeAcceleration(newAcceleration);
        log.info("Real-time simulation {} accelerated to {}x", sessionId, newAcceleration);
      }
      default -> {
        log.warn("Real-time simulation {} received unknown command: {}", sessionId, command);
      }
    }
  }

  // Copy necessary methods from SimulationEngine for real-time operations
  private HashMap<String,Double> updateSystemState(Duration timeStep) {
    // Incident processing for real-time simulation
    if (plgNetwork != null && incidentManager != null) {
      for (Truck truck : plgNetwork.getTrucks()) {
        if (truck.getStatus() == TruckState.BROKEN_DOWN) {
          Incident recoveredIncident = incidentRepository.stream()
              .filter(incident -> incident.getTruckCode().equals(truck.getCode()))
              .findFirst()
              .orElse(null);
          if (recoveredIncident != null) {
            boolean recovered = incidentManager.handleMaintenanceDelay(truck, simulatedTime, recoveredIncident);
            if (recovered) {
              log.info("Truck {} recovered from incident {} at {}", truck.getCode(), recoveredIncident.getType(),
                  simulatedTime);
            }
          }
          continue;
        }
        if (truck.getStatus() == TruckState.MAINTENANCE) {
          Incident recoveredIncident = incidentRepository.stream()
              .filter(incident -> incident.getTruckCode().equals(truck.getCode()))
              .findFirst()
              .orElse(null);
          if (recoveredIncident != null) {
            if (recoveredIncident.getExpectedRecovery() != null
                && recoveredIncident.getExpectedRecovery().isAfter(simulatedTime)) {
              truck.endMaintenance(simulatedTime);
              recoveredIncident.setStatus(IncidentStatus.RESOLVED);
              log.info("Truck {} maintenance ended for incident {} at {}", truck.getCode(), recoveredIncident.getType(),
                  simulatedTime);
            }
          }
          continue;
        }
      }
    }
    // Continue with normal system state update
    if (activeRoutes == null) {
      return null;
    }
    if (activeRoutes.getStops().isEmpty() || activeRoutes.getPaths().isEmpty()) {
      return null;
    }
    
    HashMap<String, Double> truckProgress = new HashMap<>();
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
          double progress = updateTruckLocationDuringTravel(truck, stops, paths, currentStopIndex - 1);
          truckProgress.put(truck.getId(), progress);
        }
      }

      // Check if all trucks have completed their routes and clear active routes if
      // needed
      checkAndClearCompletedRoutes();
    }
    return truckProgress;
  }

  /**
   * Checks if all trucks have completed their assigned routes and clears active
   * routes if so.
   * This prevents stale route data from being displayed when all deliveries are
   * done.
   */
  private void checkAndClearCompletedRoutes() {
    if (activeRoutes == null || activeRoutes.getStops().isEmpty()) {
      return;
    }

    // Check if all trucks have completed their routes
    boolean allTrucksCompleted = true;
    for (Truck truck : plgNetwork.getTrucks()) {
      if (truck.getStatus() == TruckState.MAINTENANCE || truck.getStatus() == TruckState.BROKEN_DOWN) {
        continue; // Skip trucks that are not operational
      }

      List<Stop> stops = activeRoutes.getStops().getOrDefault(truck.getId(), List.of());
      if (stops.size() <= 1) {
        continue; // No real route for this truck
      }

      int currentStopIndex = truckCurrentStopIndex.getOrDefault(truck.getId(), 0);
      if (currentStopIndex < stops.size()) {
        allTrucksCompleted = false;
        break;
      }
    }

    // If all trucks have completed their routes, clear the active routes
    if (allTrucksCompleted) {
      log.info("All trucks have completed their routes - clearing active routes");
      synchronized (routesLock) {
        this.activeRoutes = null;
        truckCurrentStopIndex.clear();
      }
    }
  }

  /**
   * Checks if there are no active orders and clears routes if appropriate.
   * This helps clean up stale route data when no orders are being processed.
   */
  private void checkAndClearRoutesIfNoActiveOrders() {
    if (activeRoutes == null) {
      return;
    }

    // Check if there are any active orders (PENDING, CALCULATING, or IN_PROGRESS)
    boolean hasActiveOrders = realTimeOrderRepository.getAllOrders().stream()
        .anyMatch(order -> order.getStatus() == OrderStatus.PENDING ||
            order.getStatus() == OrderStatus.CALCULATING ||
            order.getStatus() == OrderStatus.IN_PROGRESS);

    if (!hasActiveOrders) {
      log.info("No active orders remaining - clearing active routes");
      synchronized (routesLock) {
        this.activeRoutes = null;
        truckCurrentStopIndex.clear();
      }

      // Set all operational trucks to IDLE
      for (Truck truck : plgNetwork.getTrucks()) {
        if (truck.getStatus() == TruckState.ACTIVE) {
          truck.setStatus(TruckState.IDLE);
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
    stop.setArrived(true);
  }

  private void handleIncidentArrival(Truck truck, Stop stop) {
    // Handle incident arrival logic here
    log.info("Truck {} arrived at incident stop {}", truck.getId(), stop.getNode().getId());
    Truck incidentedTruck = plgNetwork.getTrucks().stream()
        .filter(t -> t.getCode().equals(stop.getNode().getId()))
        .findFirst().orElse(null);

    int availableGLP = Math.min(incidentedTruck.getCurrentCapacity(),
        truck.getMaxCapacity() - truck.getCurrentCapacity());
    int avaibleFuel = (int) Math.min(incidentedTruck.getCurrentFuel(),
        truck.getFuelCapacity() - truck.getCurrentFuel());

    truck.setCurrentCapacity(truck.getCurrentCapacity() + availableGLP);
    truck.setCurrentFuel(truck.getCurrentFuel() + avaibleFuel);
    incidentedTruck.setCurrentCapacity(incidentedTruck.getCurrentCapacity() - availableGLP);
    incidentedTruck.setCurrentFuel(incidentedTruck.getCurrentFuel() - avaibleFuel);
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

  private double updateTruckLocationDuringTravel(Truck truck, List<Stop> stops, List<Path> paths,
      int currentStopIndex) {
    if (currentStopIndex >= stops.size() - 1 || currentStopIndex >= paths.size()) {
      return 0; // No more paths to travel
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
      log.info("Algorithm interval adjusted from {} to {} based on order arrival rate: {:.2f} orders/hour",
          previousInterval, newInterval, orderArrivalRate);
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
              simulationConfig.getAlgorithmTime(), plgNetwork.getIncidents()));
    } else {
      log.debug("No planification requested - no orders in CALCULATING state");
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
      synchronized (routesLock) {
        this.activeRoutes = null;
        truckCurrentStopIndex.clear();
      }

      List<Order> calculatingOrders = realTimeOrderRepository.getAllOrders().stream()
          .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
          .toList();
      calculatingOrders.forEach(order -> {
        realTimeOrderRepository.updateOrderStatus(order.getId(), OrderStatus.PENDING);
      });

      log.info("No routes received from planification - cleared active routes and reset {} orders to PENDING",
          calculatingOrders.size());
      return;
    }

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

    // Get all order IDs that are part of the active routes
    Set<String> ordersInRoutes = new HashSet<>();
    for (List<Stop> stops : routes.getStops().values()) {
      for (Stop stop : stops) {
        if (stop.getNode().getType() == NodeType.DELIVERY) {
          ordersInRoutes.add(stop.getNode().getId());
        }
      }
    }

    // Check for IN_PROGRESS orders that are no longer in routes
    List<Order> inProgressOrders = realTimeOrderRepository.getAllOrders().stream()
        .filter(order -> order.getStatus() == OrderStatus.IN_PROGRESS)
        .toList();

    log.info("Route validation: {} orders in routes, {} orders currently IN_PROGRESS",
        ordersInRoutes.size(), inProgressOrders.size());

    int resetCount = 0;
    for (Order order : inProgressOrders) {
      if (!ordersInRoutes.contains(order.getId())) {
        realTimeOrderRepository.updateOrderStatus(order.getId(), OrderStatus.PENDING);
        resetCount++;
        log.info("Order {} reset to PENDING - no longer in any route", order.getId());
      } else {
        log.debug("Order {} remains IN_PROGRESS - found in new routes", order.getId());
      }
    }

    if (resetCount > 0) {
      log.info("Reset {} IN_PROGRESS orders to PENDING because they were not included in new routes", resetCount);
    }

    // Set CALCULATING orders to IN_PROGRESS
    List<Order> calculatingOrders = realTimeOrderRepository.getAllOrders().stream()
        .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
        .toList();

    calculatingOrders.forEach(order -> {
      realTimeOrderRepository.updateOrderStatus(order.getId(), OrderStatus.IN_PROGRESS);
      log.debug("Order {} set from CALCULATING to IN_PROGRESS", order.getId());
    });

    // Final status summary
    long finalPendingCount = realTimeOrderRepository.getAllOrders().stream()
        .filter(order -> order.getStatus() == OrderStatus.PENDING).count();
    long finalInProgressCount = realTimeOrderRepository.getAllOrders().stream()
        .filter(order -> order.getStatus() == OrderStatus.IN_PROGRESS).count();
    long finalCompletedCount = realTimeOrderRepository.getAllOrders().stream()
        .filter(order -> order.getStatus() == OrderStatus.COMPLETED).count();

    log.info(
        "Planification result processed: {} CALCULATING->IN_PROGRESS, Final status - PENDING: {}, IN_PROGRESS: {}, COMPLETED: {}",
        calculatingOrders.size(), finalPendingCount, finalInProgressCount, finalCompletedCount);

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
   * Check if blockades have changed since the last check
   * 
   * @return true if blockades have changed, false otherwise
   */
  private boolean checkForBlockadeChanges() {
    // Only check blockades periodically to avoid excessive file I/O
    if (lastBlockadeCheck != null &&
        simulatedTime.isBefore(lastBlockadeCheck.plus(BLOCKADE_CHECK_INTERVAL))) {
      return false;
    }

    try {
      List<Roadblock> currentBlockades = blockadeProcessor.getActiveBlockades(simulatedTime);
      boolean hasChanged = blockadeProcessor.hasActiveBlockadesChanged(simulatedTime, lastActiveBlockades);

      if (hasChanged) {
        log.info("Blockade changes detected at {}: {} active blockades",
            simulatedTime, currentBlockades.size());
        lastActiveBlockades = currentBlockades;
      }

      lastBlockadeCheck = simulatedTime;
      return hasChanged;
    } catch (Exception e) {
      log.warn("Error checking blockade changes: {}", e.getMessage());
      return false;
    }
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
