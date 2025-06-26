package com.hyperlogix.server.features.operation.repository;

import com.hyperlogix.server.domain.Order;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface RealTimeOrderRepository {
  void addOrder(Order order);

  List<Order> getAllOrders();

  List<Order> getPendingOrders();

  void updateOrderStatus(String orderId, com.hyperlogix.server.domain.OrderStatus status);

  Order getOrderById(String orderId);

  void removeOrder(String orderId);

  int size();

  void clear();
}
