package com.hyperlogix.server.features.operation.services;

import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.TruckState;
import com.hyperlogix.server.features.operation.repository.RealTimeOrderRepository;
import com.hyperlogix.server.services.simulation.SimulationService;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class RealTimeOperationService {
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
  @Async
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

  public boolean isSimulationInitialized() {
    return simulationInitialized.get();
  }

  public void triggerManualReplanification() {
    if (!simulationInitialized.get()) {
      throw new IllegalStateException("Simulation not yet initialized");
    }

    simulationService.triggerImmediatePlanification(MAIN_SESSION_ID);
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
