package com.hyperlogix.server.features.orders.controllers;

import com.hyperlogix.server.features.orders.usecases.GetOrdersUseCase;
import com.hyperlogix.server.domain.Order;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {
  private final GetOrdersUseCase getOrdersUseCase;

  public OrderController(GetOrdersUseCase getOrdersUseCase) {
    this.getOrdersUseCase = getOrdersUseCase;
  }

  @GetMapping
  public List<Order> list() {
    return getOrdersUseCase.getAllOrders();
  }
}
