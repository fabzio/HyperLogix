package com.hyperlogix.server.features.operation.repository;

import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.domain.OrderStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Repository
public class RealTimeOrderRepositoryImpl implements RealTimeOrderRepository {
  private final ConcurrentMap<String, Order> orders = new ConcurrentHashMap<>();

  @Override
  public void addOrder(Order order) {
    orders.put(order.getId(), order);
  }

  @Override
  public List<Order> getAllOrders() {
    return orders.values().stream().collect(Collectors.toList());
  }

  @Override
  public List<Order> getPendingOrders() {
    return orders.values().stream()
        .filter(order -> order.getStatus() == OrderStatus.PENDING)
        .collect(Collectors.toList());
  }

  @Override
  public void updateOrderStatus(String orderId, OrderStatus status) {
    Order order = orders.get(orderId);
    if (order != null) {
      order.setStatus(status);
    }
  }

  @Override
  public Order getOrderById(String orderId) {
    return orders.get(orderId);
  }

  @Override
  public void removeOrder(String orderId) {
    orders.remove(orderId);
  }

  @Override
  public boolean existsById(String orderId) {
    return orders.containsKey(orderId);
  }

  @Override
  public int size() {
    return orders.size();
  }

  @Override
  public void clear() {
    orders.clear();
  }
}
