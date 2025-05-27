package com.hyperlogix.server.features.simulation.usecases.in;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StartSimulationUseCaseIn {
  String simulationId = "fabzio";
  LocalDateTime startTimeOrders = LocalDateTime.of(2025, 1, 1, 1, 24);
  LocalDateTime endTimeOrders = LocalDateTime.of(2023, 1, 8, 1, 24);

}
