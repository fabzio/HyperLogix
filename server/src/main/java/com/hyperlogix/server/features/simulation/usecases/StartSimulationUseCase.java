package com.hyperlogix.server.features.simulation.usecases;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Roadblock;
import com.hyperlogix.server.domain.Station;
import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.features.blocks.repository.RoadblockRepository;
import com.hyperlogix.server.features.blocks.utils.BlockMapper;
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
        .map(BlockMapper::mapToDomain).toList();

    PLGNetwork plgNetwork = new PLGNetwork(trucks, stations, orders, List.of(), roadblocks);

    simulationService.startSimulation(req.getSimulationId(), plgNetwork);
  }
}
