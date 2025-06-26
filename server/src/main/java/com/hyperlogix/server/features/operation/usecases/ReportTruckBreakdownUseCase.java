package com.hyperlogix.server.features.operation.usecases;

import com.hyperlogix.server.features.operation.services.RealTimeOperationService;
import org.springframework.stereotype.Service;

@Service
public class ReportTruckBreakdownUseCase {
  private final RealTimeOperationService realTimeOperationService;

  public ReportTruckBreakdownUseCase(RealTimeOperationService realTimeOperationService) {
    this.realTimeOperationService = realTimeOperationService;
  }

  public void reportBreakdown(String truckId, String reason) {
    realTimeOperationService.reportTruckBreakdown(truckId, reason);
  }
}
