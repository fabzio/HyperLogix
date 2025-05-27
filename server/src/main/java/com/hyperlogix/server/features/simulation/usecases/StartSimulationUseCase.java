package com.hyperlogix.server.features.simulation.usecases;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.features.orders.repository.OrderRepository;
import com.hyperlogix.server.features.orders.utils.OrderMapper;
import com.hyperlogix.server.features.simulation.usecases.in.StartSimulationUseCaseIn;
import com.hyperlogix.server.services.simulation.SimulationService;

@Service
public class StartSimulationUseCase {
  private final SimulationService simulationService;
  private final OrderRepository orderRepository;

  public StartSimulationUseCase(SimulationService simulationService, OrderRepository orderRepository) {
    this.simulationService = simulationService;
    this.orderRepository = orderRepository;
  }

  public void startSimulation(StartSimulationUseCaseIn req) {
    LocalDateTime startTimeOrders = req.getStartTimeOrders();
    LocalDateTime endTimeOrders = req.getEndTimeOrders();

    List<Order> list = orderRepository.findByDateBetween(startTimeOrders, endTimeOrders).stream()
        .map(OrderMapper::mapToDomain).toList();

    simulationService.startSimulation(req.getSimulationId(), list);
  }
}
