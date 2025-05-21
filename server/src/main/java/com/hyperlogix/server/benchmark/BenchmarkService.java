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
import java.io.FileWriter;
import java.io.IOException;

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

  public PLGNetwork loadNetwork(String filePath, LocalDateTime startTime, LocalDateTime endTime) {
    System.out.println("Loading Network for file: " + filePath);
    List<String> orderFilePaths = List.of(filePath);

    List<Order> loadedOrders = MockData.loadOrdersFromFiles(orderFilePaths, 50); // Adjusted to match the method
                                                                                 // signature
    System.out
        .println("Loaded " + loadedOrders.size() + " orders from file (start=" + startTime + ", end=" + endTime + ").");

    PLGNetwork baseNetwork = MockData.mockNetwork();
    return new PLGNetwork(
        baseNetwork.getTrucks(),
        baseNetwork.getStations(),
        loadedOrders,
        baseNetwork.getIncidents(),
        baseNetwork.getRoadblocks());
  }

  public void startBenchmark() {
    System.out.println("Starting Benchmark...");

    String dataDir = "src/main/java/com/hyperlogix/server/benchmark/pedidos.20250419/";
    List<String> orderFilePaths = List.of(
        dataDir + "ventas202501.txt",
        dataDir + "ventas202502.txt",
        dataDir + "ventas202503.txt",
        dataDir + "ventas202505.txt",
        dataDir + "ventas202506.txt",
        dataDir + "ventas202507.txt",
        dataDir + "ventas202508.txt",
        dataDir + "ventas202509.txt",
        dataDir + "ventas202510.txt",
        dataDir + "ventas202511.txt",
        dataDir + "ventas202512.txt",
        dataDir + "ventas202601.txt",
        dataDir + "ventas202603.txt",
        dataDir + "ventas202604.txt",
        dataDir + "ventas202605.txt",
        dataDir + "ventas202606.txt",
        dataDir + "ventas202607.txt",
        dataDir + "ventas202608.txt",
        dataDir + "ventas202610.txt",
        dataDir + "ventas202611.txt",
        dataDir + "ventas202612.txt");

    String csvFilePath = "benchmark_results.csv";
    try (FileWriter csvWriter = new FileWriter(csvFilePath)) {
      csvWriter.append("mes,algoritmo,corrida,valor_objetivo\n");

      for (int month = 1; month <= orderFilePaths.size(); month++) {
        String filePath = orderFilePaths.get(month - 1);
        List<Order> loadedOrders = MockData.loadOrdersFromFiles(List.of(filePath), 50);
        if (loadedOrders.isEmpty()) {
          System.out.println("No orders found in file: " + filePath);
          continue;
        }

        LocalDateTime firstOrderTime = loadedOrders.get(0).getDate(); // Use the first order's arrival date

        PLGNetwork network = new PLGNetwork(
            MockData.mockNetwork().getTrucks(),
            MockData.mockNetwork().getStations(),
            loadedOrders,
            MockData.mockNetwork().getIncidents(),
            MockData.mockNetwork().getRoadblocks());

        System.out.println("\n--- Running Benchmark for file: " + filePath + " ---");

        // Ant Colony Optimizer
        AntColonyConfig antConfig = new AntColonyConfig(4, 10, 1.0, 2, 0.5, 100, 0.1);
        Optimizer antOptimizer = new AntColonyOptmizer(antConfig);
        BenchmarkResult antResult = runBenchmark(antOptimizer, network, firstOrderTime, Duration.ofSeconds(10));
        writeResultsToCsv(csvWriter, month, "ACO", antResult);

        // Genetic Algorithm Optimizer
        GeneticConfig geneticConfig = new GeneticConfig(4, 10, 1, 0, 0.8, 0.1);
        Optimizer geneticOptimizer = new GeneticOptimizer(geneticConfig);
        BenchmarkResult geneticResult = runBenchmark(geneticOptimizer, network, firstOrderTime,
            Duration.ofSeconds(10));
        writeResultsToCsv(csvWriter, month, "GA", geneticResult);
      }

      System.out.println("\nBenchmark Finished. Results saved to " + csvFilePath);
    } catch (IOException e) {
      System.err.println("Error writing to CSV file: " + e.getMessage());
    }
  }

  private void writeResultsToCsv(FileWriter csvWriter, int month, String algorithm, BenchmarkResult result)
      throws IOException {
    for (int i = 0; i < result.costs.size(); i++) {
      csvWriter.append(String.format("%d,%s,%d,%.2f\n", month, algorithm, i + 1, result.costs.get(i)));
    }
  }

  private BenchmarkResult runBenchmark(Optimizer optimizer, PLGNetwork network, LocalDateTime startTime,
      Duration maxDuration) {
    List<Double> costs = new ArrayList<>();
    List<Long> times = new ArrayList<>();

    for (int i = 0; i < NUM_RUNS; i++) {
      System.out.print("Run " + (i + 1) + "/" + NUM_RUNS + "... \n");

      // Clone the network and reinitialize the context for each iteration
      PLGNetwork clonedNetwork = network.clone();
      OptimizerContext context = new OptimizerContext(clonedNetwork, startTime);

      long runStartTime = System.nanoTime();
      OptimizerResult result = optimizer.run(context, maxDuration);
      long runEndTime = System.nanoTime();

      costs.add(result.getCost());
      times.add(Duration.ofNanos(runEndTime - runStartTime).toMillis());

      this.messagingTemplate.convertAndSend(
          "/topic/benchmark",
          String.format("Run %d/%d: Cost = %.2f, Time = %d ms", i + 1, NUM_RUNS, result.getCost(),
              Duration.ofNanos(runEndTime - runStartTime).toMillis()));
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
