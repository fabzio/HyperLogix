package com.hyperlogix.server.features.simulation.dtos;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class StartSimulationRequest {
  private LocalDateTime startTimeOrders;
  private LocalDateTime endTimeOrders;
  private String mode;
}
