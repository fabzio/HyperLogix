package com.hyperlogix.server.benchmark;

import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.mock.MockData;
import com.hyperlogix.server.optimizer.Optimizer;
import com.hyperlogix.server.optimizer.OptimizerContext;
import com.hyperlogix.server.optimizer.OptimizerResult;
import com.hyperlogix.server.optimizer.AntColony.AntColonyConfig;
import com.hyperlogix.server.optimizer.AntColony.AntColonyOptmizer;
import com.hyperlogix.server.optimizer.Genetic.GeneticConfig;
import com.hyperlogix.server.optimizer.Genetic.GeneticOptimizer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Benchmark {

  private static final int NUM_RUNS = 30; // Number of runs for statistical significance
  private static final double T_VALUE_95_29DF = 2.045; // t-value for 95% confidence, 29 degrees of freedom (n-1)

  public static void main(String[] args) {
    System.out.println("Starting Benchmark...");

    PLGNetwork network = MockData.mockNetwork();
    LocalDateTime startTime = LocalDateTime.now(); // Consistent start time for context
    Duration maxDuration = Duration.ofSeconds(10); // Max time per optimizer run

    // Configure Optimizers
    AntColonyConfig antConfig = new AntColonyConfig(4, 10, 1.0, 2, 0.5, 100, 0.1);
    Optimizer antOptimizer = new AntColonyOptmizer(antConfig);

    GeneticConfig geneticConfig = new GeneticConfig(4, 10, 
        1,
         0,
        1,
        0.1);
    Optimizer geneticOptimizer = new GeneticOptimizer(geneticConfig);

    System.out.println("\n--- Running Ant Colony Optimizer ---");
    BenchmarkResult antResult = runBenchmark(antOptimizer, network, startTime, maxDuration);
    printResults("Ant Colony", antResult);

    System.out.println("\n--- Running Genetic Algorithm Optimizer ---");
    BenchmarkResult geneticResult = runBenchmark(geneticOptimizer, network, startTime, maxDuration);
    printResults("Genetic Algorithm", geneticResult);

    System.out.println("\nBenchmark Finished.");
  }

  private static BenchmarkResult runBenchmark(Optimizer optimizer, PLGNetwork network, LocalDateTime startTime,
      Duration maxDuration) {
    List<Double> costs = new ArrayList<>();
    List<Long> times = new ArrayList<>(); // in milliseconds

    OptimizerContext context = new OptimizerContext(network.clone(), startTime); // Use clone for each run if optimizer
                                                                                 // modifies it

    for (int i = 0; i < NUM_RUNS; i++) {
      System.out.print("Run " + (i + 1) + "/" + NUM_RUNS + "... ");
      long runStartTime = System.nanoTime();
      OptimizerResult result = optimizer.run(context, maxDuration, true); // run without verbose
      long runEndTime = System.nanoTime();

      costs.add(result.getCost());
      times.add(Duration.ofNanos(runEndTime - runStartTime).toMillis());
      System.out.println(
          "Cost: " + result.getCost() + ", Time: " + Duration.ofNanos(runEndTime - runStartTime).toMillis() + "ms");

      // Reset context if necessary, e.g., if the network state was modified by the
      // optimizer run
      context = new OptimizerContext(network.clone(), startTime);
    }
    return new BenchmarkResult(costs, times);
  }

  private static void printResults(String algorithmName, BenchmarkResult result) {
    double meanCost = calculateMean(result.costs);
    double stdDevCost = calculateStdDev(result.costs, meanCost);
    double confidenceIntervalCostMargin = T_VALUE_95_29DF * (stdDevCost / Math.sqrt(NUM_RUNS));
    double lowerBoundCost = meanCost - confidenceIntervalCostMargin;
    double upperBoundCost = meanCost + confidenceIntervalCostMargin;

    double meanTime = calculateMeanLong(result.times);
    double stdDevTime = calculateStdDevLong(result.times, meanTime);
    double confidenceIntervalTimeMargin = T_VALUE_95_29DF * (stdDevTime / Math.sqrt(NUM_RUNS));
    double lowerBoundTime = meanTime - confidenceIntervalTimeMargin;
    double upperBoundTime = meanTime + confidenceIntervalTimeMargin;

    System.out.println("\nResults for " + algorithmName + ":");
    System.out.println("  Cost:");
    System.out.println("    Mean: " + meanCost);
    System.out.println("    Standard Deviation: " + stdDevCost);
    System.out.println("    95% Confidence Interval: [" + lowerBoundCost + ", " + upperBoundCost + "]");
    System.out.println("  Execution Time (ms):");
    System.out.println("    Mean: " + meanTime);
    System.out.println("    Standard Deviation: " + stdDevTime);
    System.out.println("    95% Confidence Interval: [" + lowerBoundTime + ", " + upperBoundTime + "]");
  }

  private static double calculateMean(List<Double> data) {
    return data.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
  }

  private static double calculateMeanLong(List<Long> data) {
    return data.stream().mapToLong(Long::longValue).average().orElse(Double.NaN);
  }

  private static double calculateStdDev(List<Double> data, double mean) {
    double sumSqDiff = data.stream().mapToDouble(d -> Math.pow(d - mean, 2)).sum();
    return Math.sqrt(sumSqDiff / (data.size() - 1));
  }

  private static double calculateStdDevLong(List<Long> data, double mean) {
    double sumSqDiff = data.stream().mapToDouble(d -> Math.pow(d - mean, 2)).sum();
    return Math.sqrt(sumSqDiff / (data.size() - 1));
  }

  private static class BenchmarkResult {
    final List<Double> costs;
    final List<Long> times;

    BenchmarkResult(List<Double> costs, List<Long> times) {
      this.costs = costs;
      this.times = times;
    }
  }
}
