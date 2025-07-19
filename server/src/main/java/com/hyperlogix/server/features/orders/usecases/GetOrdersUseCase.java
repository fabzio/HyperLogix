package com.hyperlogix.server.features.orders.usecases;

import com.hyperlogix.server.domain.Order;

import java.util.List;

public interface GetOrdersUseCase {
  List<Order> getAllOrders();
}

