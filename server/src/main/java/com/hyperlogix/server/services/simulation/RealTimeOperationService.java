package com.hyperlogix.server.services.simulation;

import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.TruckState;
import com.hyperlogix.server.features.operation.repository.RealTimeOrderRepository;
import com.hyperlogix.server.features.stations.repository.StationRepository;
import com.hyperlogix.server.features.trucks.repository.TruckRepository;
import com.hyperlogix.server.features.stations.utils.StationMapper;
import com.hyperlogix.server.features.trucks.utils.TruckMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class RealTimeOperationService {
  public void reportIncident(com.hyperlogix.server.features.operation.dtos.ReportIncidentRequest request) {
    RealTimeSimulationEngine engine = simulationService.getRealTimeSimulationEngines().get(MAIN_SESSION_ID);
    if (engine != null) {
      engine.handleIncidentReport(request.getTruckCode(), request.getIncidentType(), request.getTurn());
    } else {
      log.warn("No real-time simulation engine found for session: {}", MAIN_SESSION_ID);
    }
  }

  private static final Logger log = LoggerFactory.getLogger(RealTimeOperationService.class);
  private static final String MAIN_SESSION_ID = "main";
  private static final int MAX_RETRY_ATTEMPTS = 10;
  private static final long RETRY_DELAY_MS = 2000; // 2 seconds

  private final SimulationService simulationService;
  private final RealTimeOrderRepository realTimeOrderRepository;
  private final StationRepository stationRepository;
  private final TruckRepository truckRepository;
  private final AtomicBoolean simulationInitialized = new AtomicBoolean(false);

  public RealTimeOperationService(SimulationService simulationService,
      RealTimeOrderRepository realTimeOrderRepository,
      StationRepository stationRepository,
      TruckRepository truckRepository) {
    this.simulationService = simulationService;
    this.realTimeOrderRepository = realTimeOrderRepository;
    this.stationRepository = stationRepository;
    this.truckRepository = truckRepository;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Async("asyncExecutor")
  public void onApplicationReady() {
    CompletableFuture.runAsync(this::initializeMainSimulationWithRetry);
  }

  private void initializeMainSimulationWithRetry() {
    for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
      try {
        log.info("Attempting to initialize main real-time simulation (attempt {}/{})", attempt, MAX_RETRY_ATTEMPTS);

        PLGNetwork network = buildNetworkFromRepositories();

        // Check if we have loaded data
        if (network.getTrucks().isEmpty() || network.getStations().isEmpty()) {
          log.warn("Repositories not yet populated - trucks: {}, stations: {}",
              network.getTrucks().size(), network.getStations().size());

          if (attempt < MAX_RETRY_ATTEMPTS) {
            Thread.sleep(RETRY_DELAY_MS);
            continue;
          } else {
          }
        }

        // Start the main simulation with 1:1 acceleration (real-time)
        simulationService.startRealTimeSimulation(MAIN_SESSION_ID, network, realTimeOrderRepository);
        simulationInitialized.set(true);

        log.info("Main real-time simulation started successfully with session ID: {} - trucks: {}, stations: {}",
            MAIN_SESSION_ID, network.getTrucks().size(), network.getStations().size());
        return;

      } catch (Exception e) {
        log.error("Error initializing simulation (attempt {}/{}): {}", attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
        if (attempt < MAX_RETRY_ATTEMPTS) {
          try {
            Thread.sleep(RETRY_DELAY_MS);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
          }
        }
      }
    }

  }

  public void addOrder(Order order) {
    // Ensure simulation is initialized before adding orders
    if (!simulationInitialized.get()) {
      log.warn("Simulation not yet initialized, order {} will be queued", order.getId());
    }

    realTimeOrderRepository.addOrder(order);
    log.info("Added order {} to real-time simulation", order.getId());

    // Trigger immediate update and planification to notify frontend
    if (simulationInitialized.get()) {
      simulationService.triggerImmediatePlanification(MAIN_SESSION_ID);
      log.debug("Triggered immediate planification after adding order {}", order.getId());
    }
  }

  public void reportTruckBreakdown(String truckId, String reason) {
    if (!simulationInitialized.get()) {
      return;
    }

    simulationService.updateTruckState(MAIN_SESSION_ID, truckId, TruckState.BROKEN_DOWN);
  }

  public void reportTruckMaintenance(String truckId, String reason) {
    if (!simulationInitialized.get()) {
      return;
    }

    simulationService.updateTruckState(MAIN_SESSION_ID, truckId, TruckState.MAINTENANCE);
  }

  public void restoreTruckToIdle(String truckId) {
    if (!simulationInitialized.get()) {
      return;
    }

    simulationService.updateTruckState(MAIN_SESSION_ID, truckId, TruckState.IDLE);
  }

  /**
   * Schedules a future maintenance for a truck
   * 
   * @param truckId         The ID of the truck to schedule maintenance for
   * @param maintenanceTime The time when maintenance should occur
   */
  public void scheduleTruckMaintenance(String truckId, java.time.LocalDateTime maintenanceTime) {
    if (!simulationInitialized.get()) {
      throw new IllegalStateException("Simulation not yet initialized");
    }

    simulationService.scheduleTruckMaintenance(MAIN_SESSION_ID, truckId, maintenanceTime);
    log.info("Scheduled maintenance for truck {} at {}", truckId, maintenanceTime);
  }

  /**
   * Schedules an immediate maintenance for a truck (starts now)
   * This will interrupt any current route and put the truck in maintenance
   * 
   * @param truckId The ID of the truck to put in immediate maintenance
   * @param reason  The reason for the maintenance
   */
  public void scheduleImmediateMaintenance(String truckId, String reason) {
    if (!simulationInitialized.get()) {
      throw new IllegalStateException("Simulation not yet initialized");
    }

    // Schedule maintenance to start immediately (current simulation time)
    simulationService.updateTruckState(MAIN_SESSION_ID, truckId, TruckState.MAINTENANCE);
    log.info("Scheduled immediate maintenance for truck {} - reason: {}", truckId, reason);
  }

  public boolean isSimulationInitialized() {
    return simulationInitialized.get();
  }

  public void triggerManualReplanification() {
    if (!simulationInitialized.get()) {
      throw new IllegalStateException("Simulation not yet initialized");
    }

    simulationService.triggerImmediatePlanification(MAIN_SESSION_ID);
  }

  /**
   * Sends a command to the real-time simulation
   * 
   * @param command The command to send (PAUSE, RESUME, ACCELERATE, DESACCELERATE)
   */
  public void sendSimulationCommand(String command) {
    if (!simulationInitialized.get()) {
      throw new IllegalStateException("Simulation not yet initialized");
    }

    log.info("Sending command '{}' to real-time simulation", command);
    simulationService.sendCommand(MAIN_SESSION_ID, command);
  }

  /**
   * Cancels an order from the real-time simulation system
   * 
   * @param orderId The ID of the order to cancel
   * @return true if the order was found and cancelled, false if not found
   */
  public boolean cancelOrder(String orderId) {
    if (!simulationInitialized.get()) {
      throw new IllegalStateException("Simulation not yet initialized");
    }

    // Check if order exists
    if (!realTimeOrderRepository.existsById(orderId)) {
      log.warn("Attempted to cancel non-existent order: {}", orderId);
      return false;
    }

    // Remove the order from the repository
    realTimeOrderRepository.removeOrder(orderId);
    log.info("Order {} cancelled successfully", orderId);

    // Trigger immediate replanification to adjust routes
    simulationService.triggerImmediatePlanification(MAIN_SESSION_ID);
    log.info("Triggered replanification after cancelling order {}", orderId);

    return true;
  }

  /**
   * Gets the current simulation status including acceleration info
   */
  public Map<String, Object> getSimulationStatus() {
    Map<String, Object> status = new java.util.HashMap<>();
    status.put("sessionId", MAIN_SESSION_ID);
    status.put("simulationInitialized", simulationInitialized.get());

    if (simulationInitialized.get()) {
      var simStatus = simulationService.getSimulationStatus(MAIN_SESSION_ID);
      status.put("running", simStatus.running());
      status.put("paused", simStatus.paused());
      status.put("timeAcceleration", simStatus.timeAcceleration());
      status.put("message", "Real-time operation system is running");
    } else {
      status.put("running", false);
      status.put("paused", false);
      status.put("timeAcceleration", 1.0);
      status.put("message", "Real-time operation system is initializing...");
    }

    return status;
  }

  private PLGNetwork buildNetworkFromRepositories() {
    var trucks = truckRepository.findAll().stream()
        .map(TruckMapper::mapToDomain)
        .toList();

    var stations = stationRepository.findAll().stream()
        .map(StationMapper::mapToDomain)
        .toList();

    log.info("Loaded {} trucks and {} stations from repositories", trucks.size(), stations.size());

    // Start with empty orders list - orders will be added dynamically
    List<Order> orders = new ArrayList<>();

    return new PLGNetwork(trucks, stations, orders, new ArrayList<>(), new ArrayList<>());
  }
}
