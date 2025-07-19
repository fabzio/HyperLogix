package com.hyperlogix.server.features.operation.controllers;

import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.features.operation.dtos.RegisterOrderRequest;
import com.hyperlogix.server.features.operation.dtos.TruckBreakdownRequest;
import com.hyperlogix.server.features.operation.usecases.RegisterOrderUseCase;
import com.hyperlogix.server.features.operation.usecases.ReportTruckBreakdownUseCase;
import com.hyperlogix.server.services.simulation.RealTimeOperationService;

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
        0, // assignedGLP starts at 0
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

  @PostMapping("/trucks/{truckId}/maintenance")
  public ResponseEntity<Void> reportTruckMaintenance(@PathVariable String truckId,
      @RequestBody TruckBreakdownRequest request) {
    realTimeOperationService.reportTruckMaintenance(truckId, request.getReason());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/trucks/{truckId}/restore")
  public ResponseEntity<Void> restoreTruckToIdle(@PathVariable String truckId) {
    realTimeOperationService.restoreTruckToIdle(truckId);
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
    Map<String, Object> status = realTimeOperationService.getSimulationStatus();
    status.put("systemStatus", "running");
    return ResponseEntity.ok(status);
  }

  @DeleteMapping("/orders/{orderId}")
  public ResponseEntity<Map<String, String>> cancelOrder(@PathVariable String orderId) {
    if (!realTimeOperationService.isSimulationInitialized()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Real-time operation system is not initialized yet"));
    }

    try {
      boolean cancelled = realTimeOperationService.cancelOrder(orderId);
      if (cancelled) {
        return ResponseEntity.ok()
            .body(Map.of("message", "Order '" + orderId + "' cancelled successfully"));
      } else {
        return ResponseEntity.notFound()
            .build();
      }
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Failed to cancel order: " + e.getMessage()));
    }
  }

  @PostMapping("/simulation/command")
  public ResponseEntity<Map<String, String>> sendSimulationCommand(@RequestBody Map<String, String> request) {
    String command = request.get("command");

    if (command == null || command.trim().isEmpty()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Command is required"));
    }

    if (!realTimeOperationService.isSimulationInitialized()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Real-time operation system is not initialized yet"));
    }

    try {
      realTimeOperationService.sendSimulationCommand(command.toUpperCase());
      return ResponseEntity.ok()
          .body(Map.of("message", "Command '" + command + "' sent successfully"));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Failed to send command: " + e.getMessage()));
    }
  }

  @PostMapping("/simulation/pause")
  public ResponseEntity<Map<String, String>> pauseSimulation() {
    if (!realTimeOperationService.isSimulationInitialized()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Real-time operation system is not initialized yet"));
    }

    try {
      realTimeOperationService.sendSimulationCommand("PAUSE");
      return ResponseEntity.ok()
          .body(Map.of("message", "Simulation paused successfully"));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Failed to pause simulation: " + e.getMessage()));
    }
  }

  @PostMapping("/simulation/resume")
  public ResponseEntity<Map<String, String>> resumeSimulation() {
    if (!realTimeOperationService.isSimulationInitialized()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Real-time operation system is not initialized yet"));
    }

    try {
      realTimeOperationService.sendSimulationCommand("RESUME");
      return ResponseEntity.ok()
          .body(Map.of("message", "Simulation resumed successfully"));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Failed to resume simulation: " + e.getMessage()));
    }
  }

  @PostMapping("/simulation/accelerate")
  public ResponseEntity<Map<String, String>> accelerateSimulation() {
    if (!realTimeOperationService.isSimulationInitialized()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Real-time operation system is not initialized yet"));
    }

    try {
      realTimeOperationService.sendSimulationCommand("ACCELERATE");
      return ResponseEntity.ok()
          .body(Map.of("message", "Simulation accelerated successfully"));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Failed to accelerate simulation: " + e.getMessage()));
    }
  }

  @PostMapping("/simulation/decelerate")
  public ResponseEntity<Map<String, String>> decelerateSimulation() {
    if (!realTimeOperationService.isSimulationInitialized()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Real-time operation system is not initialized yet"));
    }

    try {
      realTimeOperationService.sendSimulationCommand("DESACCELERATE");
      return ResponseEntity.ok()
          .body(Map.of("message", "Simulation decelerated successfully"));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Failed to decelerate simulation: " + e.getMessage()));
    }
  }
}
