package com.hyperlogix.server.features.operation.usecases;

import org.springframework.stereotype.Service;
import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.features.operation.services.RealTimeOperationService;

@Service
public class RegisterOrderUseCase {
  private final RealTimeOperationService realTimeOperationService;

  public RegisterOrderUseCase(RealTimeOperationService realTimeOperationService) {
    this.realTimeOperationService = realTimeOperationService;
  }

  public void registerOrder(Order order) {
    realTimeOperationService.addOrder(order);
  }
}
