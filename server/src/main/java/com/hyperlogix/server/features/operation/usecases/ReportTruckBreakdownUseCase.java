package com.hyperlogix.server.features.operation.usecases;

import org.springframework.stereotype.Service;

import com.hyperlogix.server.services.simulation.RealTimeOperationService;

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
