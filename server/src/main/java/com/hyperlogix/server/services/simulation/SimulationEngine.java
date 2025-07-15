package com.hyperlogix.server.services.simulation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import com.hyperlogix.server.domain.ActiveIncident;
import com.hyperlogix.server.domain.CompletedIncident;
import com.hyperlogix.server.domain.Incident;
import com.hyperlogix.server.domain.IncidentType;
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
import com.hyperlogix.server.services.incident.IncidentManagement;
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

  // Incident management
  private IncidentManagement incidentManager = null;
  private final Map<String, ActiveIncident> activeIncidents = new ConcurrentHashMap<>();
  private final List<CompletedIncident> completedIncidents = new ArrayList<>();
  private LocalDateTime lastPlanificationStart;  // Order arrival rate tracking

  private final Map<LocalDateTime, Integer> orderArrivalHistory = new ConcurrentHashMap<>();
  private LocalDateTime lastOrderRateCheck = null;
  private static final Duration ORDER_RATE_CHECK_WINDOW = Duration.ofMinutes(10);


  public SimulationEngine(String sessionId,
      SimulationConfig simulationConfig,
      SimulationNotifier simulationNotifier,
      List<Order> orderRepository,
      ApplicationEventPublisher eventPublisher,
      PlanificationService planificationService) {
    this.sessionId = sessionId;
    this.simulationConfig = simulationConfig;
    this.simulationNotifier = simulationNotifier;
    this.orderRepository = orderRepository;
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

      // Check and adjust algorithm interval based on order arrival rate
      if (simulatedTime.isAfter(lastOrderRateCheck.plus(ORDER_RATE_CHECK_WINDOW))) {
        adjustAlgorithmIntervalBasedOnOrderRate();
        lastOrderRateCheck = simulatedTime;
      }

      if (simulatedTime.isAfter(nextPlanningTime)) {
        int newOrderCount = getOrderBatch(simulatedTime);

        // Track order arrivals for rate calculation
        if (newOrderCount > 0) {
          orderArrivalHistory.put(simulatedTime, newOrderCount);
        }

        requestPlanification();
        if (!incidentsOrganized && activeRoutes != null) {
            incidentsOrganized = true;
        }
        nextPlanningTime = nextPlanningTime
            .plus(simulationConfig.getConsumptionInterval());
        log.info("Next planning time: {} (interval: {})", nextPlanningTime,
            simulationConfig.getCurrentAlgorithmInterval());
      }

      processIncidents();

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

  }

  private void processIncidents() {
    if (incidentManager == null || plgNetwork == null) return;

    // Procesar todos los incidentes a través del IncidentManager
    incidentManager.processIncidents(plgNetwork, simulatedTime);
  }

  
  private String getCurrentTurn(LocalDateTime time) {
    int hour = time.getHour() % 24;
    if (hour >= 0 && hour < 8) {
      return "T1";
    } else if (hour >= 8 && hour < 16) {
      return "T2";
    } else {
      return "T3";
    }
  }

  private String getIncidentTypeFromTruckStatus(TruckState status) {
    switch (status) {
      case MAINTENANCE:
        return "TYPE_1";
      case BROKEN_DOWN:
        return "TYPE_2"; // Could be TYPE_2 or TYPE_3, would need additional logic
      default:
        return "TYPE_1";
    }
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
    }    synchronized (routesLock) {
      for (Truck truck : plgNetwork.getTrucks()) {
        // Handle trucks that are completely broken down (TYPE_2/TYPE_3 incidents)
        if (truck.getStatus() == TruckState.BROKEN_DOWN) {
          continue; // These trucks need workshop repair, skip all processing
        }

        // Handle trucks in maintenance (TYPE_1 incidents) - they stay in standby
        if (truck.getStatus() == TruckState.MAINTENANCE) {
          boolean recovered = incidentManager.handleMaintenanceDelay(truck, simulatedTime);
          if (!recovered) {
            // Truck is still in maintenance, skip movement but keep route assigned
            log.trace("Truck {} in maintenance standby - route preserved", truck.getId());
            continue;
          }
          // If recovered, continue with normal processing below
          changeOrdersState(truck.getId(), OrderStatus.IN_PROGRESS);
          log.info("Truck {} recovered from maintenance, resuming route", truck.getId());
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
    
    if(truck.getStatus()== TruckState.RETURNING_TO_BASE){
      truck.setStatus(TruckState.IDLE);
      log.info("Truck {} returned to base and is now IDLE", truck.getId());
    }
    
    if(truck.getStatus()== TruckState.RETURNING_TO_BASE){
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

    // Debug: Escribir estado de todas las órdenes después de cada entrega
    try (java.io.FileWriter fw = new java.io.FileWriter("debug_deliveries_sim.txt", true)) {
      fw.write("--- Estado de órdenes tras entrega ---\n");
      for (Order o : orderRepository) {
        fw.write("Order: " + o.getId()
          + ", Requested: " + o.getRequestedGLP()
          + ", Delivered: " + o.getDeliveredGLP()
          + ", Assigned: " + o.getAssignedGLP() + "\n");
      }
      fw.write("------------------------------\n");
    } catch (Exception e) { }

    if(truck.getCurrentCapacity() == 0){
      truck.setStatus(TruckState.RETURNING_TO_BASE);
      // Limpiar rutas y paths pendientes, dejando solo la última parada alcanzada
      List<Stop> currentRoute = activeRoutes.getStops().get(truck.getId());
      if (currentRoute != null && currentRoute.size() > 0) {
        Stop lastStop = currentRoute.get(Math.max(0, truckCurrentStopIndex.getOrDefault(truck.getId(), 0)));
        activeRoutes.getStops().put(truck.getId(), new ArrayList<>(List.of(lastStop)));
      }
      List<Path> currentPaths = activeRoutes.getPaths().get(truck.getId());
      if (currentPaths != null) {
        activeRoutes.getPaths().put(truck.getId(), new ArrayList<>());
      }
      // Cambiar estado de las órdenes pendientes a CALCULATING
      changeOrdersState(truck.getId(), OrderStatus.CALCULATING);
      log.info("Truck {} set to RETURNING_TO_BASE, cleared pending routes and set orders to CALCULATING", truck.getId());
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
    Point interpolatedPosition = interpolateAlongPath(currentPath.points(), progress);    // Update truck location
    truck.setLocation(interpolatedPosition);

    // Check for incidents between 5% and 35% progress of current path
    if (progress >= 0.05 && progress <= 0.35) {
      if(incidentManager.checkAndHandleIncident(truck, simulatedTime)) 
        changeOrdersState(truck.getId(), OrderStatus.DELAYED);
    }
    log.trace("Truck {} at position ({}, {}) - progress: {:.2f}%, fuel: {:.2f}gal",
      truck.getId(), interpolatedPosition.x(), interpolatedPosition.y(), progress * 100, newFuelLevel);
  }


  private void changeOrdersState(String truckId, OrderStatus newStatus){
      // Cambiar estado A todas las órdenes asignadas actualmente a este camión
      List<Stop> assignedStops = activeRoutes.getStops().getOrDefault(truckId, List.of());
      List<String> delayedOrderIds = new ArrayList<>();
      for (Stop stop : assignedStops) {
        if (stop.getNode().getType() == NodeType.DELIVERY) {
          Order order = orderRepository.stream()
            .filter(o -> o.getId().equals(stop.getNode().getId()))
            .findFirst().orElse(null);
          if (order != null) {
            order.setStatus(newStatus);
            if (newStatus == OrderStatus.DELAYED) {
              delayedOrderIds.add(order.getId());
            }
          }
        }
      }
        // Las órdenes deben ser prioridad para este camión en la próxima planificación
        // (El planificador debe consultar truckPriorityOrders para forzar la asignación)
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
          .filter(order -> order.getStatus() == OrderStatus.IN_PROGRESS)
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
    List<CompletedIncident> completedIncidents = getCompletedIncidents();
    
    PlanificationRequestEvent request = new PlanificationRequestEvent(
        sessionId, 
        plgNetwork, 
        simulatedTime, 
        simulationConfig.getAlgorithmTime(),
        completedIncidents
    );
    
    eventPublisher.publishEvent(request);
    log.info("Planification requested for session {} with {} completed incidents", 
        sessionId, completedIncidents.size());
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

  public void onPlanificationResult(Routes routes, List<Incident> newIncidents) {
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
        this.activeRoutes = routes;
        truckCurrentStopIndex.clear();

        log.info("Received routes for {} trucks, {} total stops, {} total paths",
            routes.getStops().size(),
            routes.getStops().values().stream().mapToInt(List::size).sum(),
            routes.getPaths().values().stream().mapToInt(List::size).sum());

        // Agregar nuevos incidentes al manager
        scheduleNewIncidents(newIncidents);
        
        // Actualizar estados de camiones considerando incidentes activos
        updateTruckStatesWithRoutes(routes);
    }
}

  private void scheduleNewIncidents(List<Incident> newIncidents) {
    if (newIncidents == null || newIncidents.isEmpty()) return;

    // Agregar incidentes al network y al manager
    if (plgNetwork != null) {
        plgNetwork.getIncidents().addAll(newIncidents);
        
        if (incidentManager != null) {
            incidentManager.addNewIncidents(newIncidents);
        }
    }
    
    log.info("Scheduled {} new incidents", newIncidents.size());
}

  private void updateTruckStatesWithRoutes(Routes routes) {
    for (Truck truck : plgNetwork.getTrucks()) {
        // Verificar si el camión tiene un incidente activo
        ActiveIncident activeIncident = incidentManager.getActiveIncidentForTruck(truck.getId());
        
        if (activeIncident != null) {
            // Camión afectado por incidente - mantener estado actual
            log.debug("Truck {} has active incident {} - maintaining current state: {}", 
                truck.getId(), activeIncident.getId(), truck.getStatus());
            continue;
        }

        // Lógica normal de asignación de rutas
        List<Stop> assignedStops = routes.getStops().getOrDefault(truck.getId(), List.of());
        
        if (assignedStops.size() <= 1) {
            truck.setStatus(TruckState.IDLE);
            log.debug("Truck {} set to IDLE - no assigned routes", truck.getId());
        } else {
            // Solo cambiar a ACTIVE si no está en mantenimiento o averiado
            if (truck.getStatus() != TruckState.MAINTENANCE && 
                truck.getStatus() != TruckState.BROKEN_DOWN) {
                truck.setStatus(TruckState.ACTIVE);
                
                // Log route details
                StringBuilder routeInfo = new StringBuilder();
                for (int i = 1; i < assignedStops.size(); i++) {
                    Stop stop = assignedStops.get(i);
                    routeInfo.append(stop.getNode().getType()).append(":").append(stop.getNode().getId());
                    if (i < assignedStops.size() - 1)
                        routeInfo.append(" -> ");
                }
                log.info("Truck {} set to ACTIVE - route: {}", truck.getId(), routeInfo.toString());
            }
        }
    }
}

// Método para obtener incidentes completados para la planificación
public List<CompletedIncident> getCompletedIncidents() {
    if (incidentManager == null) return new ArrayList<>();
    return incidentManager.getCompletedIncidents();
}

// Método para obtener métricas de incidentes
public IncidentMetrics getIncidentMetrics() {
    if (incidentManager == null) {
        return new IncidentMetrics(0, 0, 0);
    }
    
    return new IncidentMetrics(
        incidentManager.getActiveIncidents().size(),
        incidentManager.getCompletedIncidents().size(),
        incidentManager.getPendingIncidents().size()
    );
}

// Clase para métricas de incidentes
public static class IncidentMetrics {
    private final int activeIncidents;
    private final int completedIncidents;
    private final int pendingIncidents;
    
    public IncidentMetrics(int activeIncidents, int completedIncidents, int pendingIncidents) {
        this.activeIncidents = activeIncidents;
        this.completedIncidents = completedIncidents;
        this.pendingIncidents = pendingIncidents;
    }
    
    // Getters
    public int getActiveIncidents() { return activeIncidents; }
    public int getCompletedIncidents() { return completedIncidents; }
    public int getPendingIncidents() { return pendingIncidents; }
}
/*
  private void processActiveIncidents() {
    Iterator<Map.Entry<String, ActiveIncident>> iterator = activeIncidents.entrySet().iterator();
    
    while (iterator.hasNext()) {
      Map.Entry<String, ActiveIncident> entry = iterator.next();
      String truckId = entry.getKey();
      ActiveIncident incident = entry.getValue();
      
      // Check if immobilization period has ended
      if (incident.isImmobilizationEnded(simulatedTime)) {
        Truck truck = plgNetwork.getTruckById(truckId);
        
        if (IncidentType.TI1.name().equals(incident.getType())) {
          // Type 1: Return to route after immobilization
          truck.setStatus(TruckState.ACTIVE);
          log.info("Truck {} immobilization ended, returning to route", truckId);
          
          // Move to completed incidents
          completedIncidents.add(new CompletedIncident(incident, simulatedTime));
          iterator.remove();
        } else {
          // Type 2 and 3: Move to workshop
          truck.setStatus(TruckState.MAINTENANCE);
          incident.setWorkshopStartTime(simulatedTime);
          log.info("Truck {} moved to workshop for {} hours", truckId, 
              incident.getWorkshopDuration().toHours());
        }
      }
      
      // Check if workshop period has ended
      if (incident.isWorkshopEnded(simulatedTime)) {
        Truck truck = plgNetwork.getTruckById(truckId);
        truck.setStatus(TruckState.IDLE);
        log.info("Truck {} workshop period ended, available for new routes", truckId);
        
        // Move to completed incidents
        completedIncidents.add(new CompletedIncident(incident, simulatedTime));
        iterator.remove();
      }
    }
  }
*/

// Method to trigger incidents during simulation (called when truck reaches incident node)
public void triggerIncident(String truckId, String nodeId, IncidentType type) {
  Truck truck = plgNetwork.getTruckById(truckId);
  if (truck == null) {
    log.warn("Cannot trigger incident - truck {} not found", truckId);
    return;
    }

    ActiveIncident incident = new ActiveIncident(
        UUID.randomUUID().toString(), // id
        truckId,                      // truckId
        getCurrentTurn(simulatedTime),// turn
        type.name(),                  // type as String
        plgNetwork.getTruckById(truckId).getLocation(), // location
        simulatedTime                 // startTime
    );
    
    activeIncidents.put(truckId, incident);
    truck.setStatus(TruckState.BROKEN_DOWN);
    
    log.info("Incident triggered for truck {} at node {} - type {}, immobilized for {} hours",
        truckId, nodeId, type, incident.getImmobilizationDuration().toHours());
    
    // Publish incident event for UI notification
    //eventPublisher.publishEvent(new IncidentTriggeredEvent(
        //sessionId, truckId, nodeId, type, simulatedTime));
  }

  // Method for manual incident registration (operations and 7-day simulation)
  public void registerIncident(String truckId, IncidentType type) {
    Truck truck = plgNetwork.getTruckById(truckId);
    if (truck == null) {
      log.warn("Cannot register incident - truck {} not found", truckId);
      return;
    }

    // Find current node of the truck
    String currentNodeId = getCurrentNodeOfTruck(truckId);
    if (currentNodeId == null) {
      log.warn("Cannot register incident - current node of truck {} not found", truckId);
      return;
    }

    triggerIncident(truckId, currentNodeId, type);
  }

  private String getCurrentNodeOfTruck(String truckId) {
    if (activeRoutes == null) return null;
    
    List<Stop> truckStops = activeRoutes.getStops().get(truckId);
    if (truckStops == null || truckStops.isEmpty()) return null;
    
    Integer currentStopIndex = truckCurrentStopIndex.get(truckId);
    if (currentStopIndex == null) return null;
    
    if (currentStopIndex < truckStops.size()) {
      return truckStops.get(currentStopIndex).getNode().getId();
    }
    
    return null;
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
}
