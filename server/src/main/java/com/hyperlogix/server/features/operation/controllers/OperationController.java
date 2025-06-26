package com.hyperlogix.server.features.operation.controllers;

import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.features.operation.dtos.RegisterOrderRequest;
import com.hyperlogix.server.features.operation.dtos.TruckBreakdownRequest;
import com.hyperlogix.server.features.operation.services.RealTimeOperationService;
import com.hyperlogix.server.features.operation.usecases.RegisterOrderUseCase;
import com.hyperlogix.server.features.operation.usecases.ReportTruckBreakdownUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/operation")
public class OperationController {
  private final RegisterOrderUseCase registerOrderUseCase;
  private final ReportTruckBreakdownUseCase reportTruckBreakdownUseCase;
  private final RealTimeOperationService realTimeOperationService;

  public OperationController(RegisterOrderUseCase registerOrderUseCase,
      ReportTruckBreakdownUseCase reportTruckBreakdownUseCase,
      RealTimeOperationService realTimeOperationService) {
    this.registerOrderUseCase = registerOrderUseCase;
    this.reportTruckBreakdownUseCase = reportTruckBreakdownUseCase;
    this.realTimeOperationService = realTimeOperationService;
  }

  @PostMapping("/orders")
  public ResponseEntity<Void> registerOrder(@RequestBody RegisterOrderRequest request) {
    Order order = new Order(
        request.getId(),
        request.getClientId(),
        request.getDate(),
        request.getLocation(),
        request.getRequestedGLP(),
        0, // deliveredGLP starts at 0
        request.getDeliveryLimit(),
        com.hyperlogix.server.domain.OrderStatus.PENDING);

    registerOrderUseCase.registerOrder(order);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/trucks/{truckId}/breakdown")
  public ResponseEntity<Void> reportTruckBreakdown(@PathVariable String truckId,
      @RequestBody TruckBreakdownRequest request) {
    reportTruckBreakdownUseCase.reportBreakdown(truckId, request.getReason());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/replan")
  public ResponseEntity<Map<String, String>> manualReplanification() {
    if (!realTimeOperationService.isSimulationInitialized()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Real-time operation system is not initialized yet"));
    }

    try {
      // Trigger immediate replanification through the simulation service
      realTimeOperationService.triggerManualReplanification();
      return ResponseEntity.ok()
          .body(Map.of("message", "Manual replanification triggered successfully"));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Failed to trigger replanification: " + e.getMessage()));
    }
  }

  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getOperationStatus() {
    Map<String, Object> status = Map.of(
        "systemStatus", "running",
        "sessionId", "main",
        "simulationInitialized", realTimeOperationService.isSimulationInitialized(),
        "message", realTimeOperationService.isSimulationInitialized()
            ? "Real-time operation system is running"
            : "Real-time operation system is initializing...");
    return ResponseEntity.ok(status);
  }
}
