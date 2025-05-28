package com.hyperlogix.server.services.simulation;

import java.time.Duration;

public record SimulationConfig(
    Duration algorithmTime,
    Duration algorithmInterval,
    int timeAcceleration,
    Duration simulationResolution) {
  public Duration getConsumptionInterval() {
    return algorithmInterval.multipliedBy(timeAcceleration);
  }
}
