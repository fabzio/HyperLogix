package com.hyperlogix.server.benchmark;

import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Order; // Added import
import com.hyperlogix.server.mock.MockData;
import com.hyperlogix.server.optimizer.Optimizer;
import com.hyperlogix.server.optimizer.OptimizerContext;
import com.hyperlogix.server.optimizer.OptimizerResult;
import com.hyperlogix.server.optimizer.Notifier; // Added import
import com.hyperlogix.server.optimizer.AntColony.AntColonyConfig;
import com.hyperlogix.server.optimizer.AntColony.AntColonyOptmizer;
import com.hyperlogix.server.optimizer.Genetic.GeneticConfig;
import com.hyperlogix.server.optimizer.Genetic.GeneticOptimizer;

import lombok.Getter;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
// Added for potential file path construction, if needed by MockData directly
// import java.nio.file.Paths; 

@Service
public class BenchmarkService {
  private final SimpMessagingTemplate messagingTemplate;
  @Getter
  private PLGNetwork network;
  private static final int NUM_RUNS = 30; // Number of runs for statistical significance
  private static final double T_VALUE_95_29DF = 2.045; // t-value for 95% confidence, 29 degrees of freedom (n-1)

  public BenchmarkService(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  public PLGNetwork loadNetwork() {
    System.out.println("Loading Network...");
    String dataDir = "src/main/java/com/hyperlogix/server/benchmark/pedidos.20250419/";
    List<String> orderFilePaths = List.of(
        dataDir + "ventas202501.txt");

    // Define offset and limit for loading orders
    int ordersOffset = 0; // Start from the first order (after skipping 'offset' orders)
    int ordersLimit = 10; // Load all orders after offset (-1 or 0 means no limit)

    List<Order> loadedOrders = MockData.loadOrdersFromFiles(orderFilePaths, ordersOffset, ordersLimit);
    System.out.println("Loaded " + loadedOrders.size() + " orders from files (offset=" + ordersOffset + ", limit="
        + ordersLimit + ").");
    PLGNetwork baseNetwork = MockData.mockNetwork(); // Gets trucks, stations etc. from original mock
    network = new PLGNetwork(
        baseNetwork.getTrucks(),
        baseNetwork.getStations(),
        loadedOrders,
        baseNetwork.getIncidents(),
        baseNetwork.getRoadblocks());
    return network;
  }

  public void startBenchmark() {
    System.out.println("Starting Benchmark...");

    LocalDateTime startTime = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0, 0);
    Duration maxDuration = Duration.ofSeconds(10);
    AntColonyConfig antConfig = new AntColonyConfig(4, 10, 1.0, 2, 0.5, 100, 0.1);
    Optimizer antOptimizer = new AntColonyOptmizer(antConfig);

    GeneticConfig geneticConfig = new GeneticConfig(4, 10,
        1,
        0,
        1,
        0.1);
    Optimizer geneticOptimizer = new GeneticOptimizer(geneticConfig);

    System.out.println("\n--- Running Ant Colony Optimizer ---");
    BenchmarkResult antResult = runBenchmark(antOptimizer, network, startTime,
        maxDuration);
    printResults("Ant Colony", antResult);

    // System.out.println("\n--- Running Genetic Algorithm Optimizer ---");
    // BenchmarkResult geneticResult = runBenchmark(geneticOptimizer, network,
    // startTime, maxDuration);
    // printResults("Genetic Algorithm", geneticResult);

    System.out.println("\nBenchmark Finished.");
  }

  private BenchmarkResult runBenchmark(Optimizer optimizer, PLGNetwork network, LocalDateTime startTime,
      Duration maxDuration) {
    List<Double> costs = new ArrayList<>();
    List<Long> times = new ArrayList<>();

    OptimizerContext context = new OptimizerContext(network.clone(), startTime);

    Notifier optimizerNotifier = message -> this.messagingTemplate.convertAndSend(
        "/topic/benchmark", // Sending optimizer specific logs to a sub-topic
        message);

    for (int i = 0; i < NUM_RUNS; i++) {
      System.out.print("Run " + (i + 1) + "/" + NUM_RUNS + "... \n");
      long runStartTime = System.nanoTime();
      OptimizerResult result = optimizer.run(context, maxDuration, optimizerNotifier); // Pass the notifier
      long runEndTime = System.nanoTime();

      costs.add(result.getCost());
      times.add(Duration.ofNanos(runEndTime - runStartTime).toMillis());
      this.messagingTemplate.convertAndSend(
          "/topic/benchmark",
          String.format("Run %d/%d: Cost = %.2f, Time = %d ms", i + 1, NUM_RUNS, result.getCost(),
              Duration.ofNanos(runEndTime - runStartTime).toMillis()));

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
