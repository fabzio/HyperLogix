package com.hyperlogix.server.features.simulation.usecases;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyperlogix.server.domain.Incident;
import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Roadblock;
import com.hyperlogix.server.domain.Station;
import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.features.blocks.repository.RoadblockRepository;
import com.hyperlogix.server.features.blocks.utils.BlockMapper;
import com.hyperlogix.server.features.incidents.repository.IncidentRepository;
import com.hyperlogix.server.features.incidents.utils.IncidentMapper;
import com.hyperlogix.server.features.orders.repository.OrderRepository;
import com.hyperlogix.server.features.orders.utils.OrderMapper;
import com.hyperlogix.server.features.simulation.usecases.in.StartSimulationUseCaseIn;
import com.hyperlogix.server.features.stations.repository.StationRepository;
import com.hyperlogix.server.features.stations.utils.StationMapper;
import com.hyperlogix.server.features.trucks.repository.TruckRepository;
import com.hyperlogix.server.features.trucks.utils.TruckMapper;
import com.hyperlogix.server.services.simulation.SimulationService;

@Service
public class StartSimulationUseCase {
  @Autowired
  private SimulationService simulationService;
  @Autowired
  private OrderRepository orderRepository;
  @Autowired
  private TruckRepository truckRepository;
  @Autowired
  private StationRepository stationRepository;
  
  @Autowired
  private RoadblockRepository roadblockRepository;
  
  @Autowired
  private IncidentRepository incidentRepository;

  public void startSimulation(StartSimulationUseCaseIn req) {
    LocalDateTime startTimeOrders = req.getStartTimeOrders();
    LocalDateTime endTimeOrders = req.getEndTimeOrders();

    List<Order> orders = orderRepository.findByDateBetweenOrderByDateAsc(startTimeOrders, endTimeOrders).stream()
        .map(OrderMapper::mapToDomain).toList();

    List<Truck> trucks = truckRepository.findAll().stream()
        .map(TruckMapper::mapToDomain).toList();
    List<Station> stations = stationRepository.findAll().stream()
        .map(StationMapper::mapToDomain).toList();
    List<Roadblock> roadblocks = roadblockRepository
        .findByStartTimeBetweenOrderByStartTimeAsc(startTimeOrders, endTimeOrders).stream()
        .map(BlockMapper::mapToDomain).toList();    List<Incident> incidents = incidentRepository.findAll().stream()
        .map(IncidentMapper::mapToDomain).toList();
    
    System.out.println("==================== SIMULATION DEBUG ====================");
    System.out.println("Trucks loaded: " + trucks.size());
    System.out.println("Stations loaded: " + stations.size());
    System.out.println("Orders loaded: " + orders.size());
    System.out.println("Roadblocks loaded: " + roadblocks.size());
    System.out.println("Incidents from database: " + incidents.size());
    if (!incidents.isEmpty()) {
        System.out.println("First incident: Truck=" + incidents.get(0).getTruckCode() + 
                           ", Type=" + incidents.get(0).getType() + 
                           ", Turn=" + incidents.get(0).getTurn());
    } else {
        System.out.println("No incidents found in database!");
    }
    
    PLGNetwork plgNetwork = new PLGNetwork(trucks, stations, orders, incidents, roadblocks);
    
    System.out.println("PLGNetwork incidents after creation: " + plgNetwork.getIncidents().size());
    System.out.println("==================== END DEBUG ====================");

    simulationService.startSimulation(req.getSimulationId(), plgNetwork, req.getMode());
  }
}
