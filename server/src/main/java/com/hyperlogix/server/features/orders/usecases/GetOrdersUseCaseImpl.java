package com.hyperlogix.server.features.orders.usecases;

import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.domain.OrderStatus;
import com.hyperlogix.server.features.orders.repository.OrderRepository;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GetOrdersUseCaseImpl implements GetOrdersUseCase {
  private final OrderRepository orderRepository;

  public GetOrdersUseCaseImpl(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  @Override
  public List<Order> getAllOrders() {
    return orderRepository.findAll().stream()
        .map(orderEntity -> new Order(
            orderEntity.getId().toString(),
            orderEntity.getClientId(),
            orderEntity.getDate(),
            orderEntity.getLocation(),
            orderEntity.getRequestedGLP(),
            orderEntity.getDeliveredGLP(),
            orderEntity.getDeliveryLimit(),
            OrderStatus.PENDING))
        .toList();
  }
}


