package com.hyperlogix.server.services.simulation;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class SimulationConfig {
  private final Duration algorithmTime;
  private final Duration algorithmInterval;
  @Setter
  private double timeAcceleration;
  private final Duration simulationResolution;

  // Dynamic interval adjustment fields
  @Setter
  private Duration currentAlgorithmInterval;
  private final Duration minAlgorithmInterval;
  private final Duration maxAlgorithmInterval;

  public SimulationConfig(Duration algorithmTime, Duration algorithmInterval, double timeAcceleration,
      Duration simulationResolution) {
    this.algorithmTime = algorithmTime;
    this.algorithmInterval = algorithmInterval;
    this.timeAcceleration = timeAcceleration;
    this.simulationResolution = simulationResolution;
    this.currentAlgorithmInterval = algorithmInterval;
    // Ensure min interval is at least as large as algorithm time
    this.minAlgorithmInterval = algorithmTime.plusSeconds(1);
    this.maxAlgorithmInterval = algorithmInterval.multipliedBy(3);
  }

  public Duration getConsumptionInterval() {
    return currentAlgorithmInterval.multipliedBy((long) timeAcceleration);
  }

  public void adjustIntervalBasedOnOrderRate(double orderArrivalRate) {
    // Base interval adjustment factor on order rate
    // Higher rate = shorter interval (more frequent planning)
    double adjustmentFactor = 1.0;

    if (orderArrivalRate > 2.0) { // High order rate
      adjustmentFactor = 0.5; // Reduce interval by 50%
    } else if (orderArrivalRate > 1.0) { // Medium order rate
      adjustmentFactor = 0.75; // Reduce interval by 25%
    } else if (orderArrivalRate < 0.3) { // Low order rate
      adjustmentFactor = 1.5; // Increase interval by 50%
    }

    Duration newInterval = algorithmInterval.multipliedBy((long) (adjustmentFactor * 100)).dividedBy(100);

    // Ensure constraints are met
    if (newInterval.compareTo(minAlgorithmInterval) < 0) {
      newInterval = minAlgorithmInterval;
    }
    if (newInterval.compareTo(maxAlgorithmInterval) > 0) {
      newInterval = maxAlgorithmInterval;
    }

    // Smooth adjustment - only change if difference is significant
    Duration currentInterval = getCurrentAlgorithmInterval();
    long diffSeconds = Math.abs(newInterval.toSeconds() - currentInterval.toSeconds());
    if (diffSeconds > 30) { // Only adjust if difference is more than 30 seconds
      this.currentAlgorithmInterval = newInterval;
    }
  }

  public Duration getMinAlgorithmInterval() {
    return minAlgorithmInterval;
  }

  public Duration getMaxAlgorithmInterval() {
    return maxAlgorithmInterval;
  }
}
