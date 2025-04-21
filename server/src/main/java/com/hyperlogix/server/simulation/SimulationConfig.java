package com.hyperlogix.server.simulation;

import java.time.Duration;
import java.time.LocalDateTime;

public record SimulationConfig(
    Duration algorithmTime,
    Duration algorithmInterval,
    int timeAcceleration,
    Duration simulationDuration,
    LocalDateTime simulationStartTime,
    Duration simulationResolution) {
  public Duration getConsumptionInterval() {
    return algorithmInterval.multipliedBy(timeAcceleration);
  }
}
