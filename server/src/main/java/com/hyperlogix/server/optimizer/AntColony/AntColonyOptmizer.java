package com.hyperlogix.server.optimizer.AntColony;

import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.optimizer.Graph;
import com.hyperlogix.server.optimizer.Optimizer;
import com.hyperlogix.server.optimizer.OptimizerContext;
import com.hyperlogix.server.optimizer.OptimizerResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class AntColonyOptmizer implements Optimizer {
  private AntColonyConfig antColonyConfig;
  private Graph graph;
  private List<Ant> ants;

  public AntColonyOptmizer(AntColonyConfig antColonyConfig) {
    this.antColonyConfig = antColonyConfig;
  }

  @Override
  public OptimizerResult run(OptimizerContext ctx, Duration maxDuration, boolean verbose) {
    graph = new Graph(ctx.plgNetwork, ctx.algorithmStartDate, antColonyConfig);
    ants = new ArrayList<>();

    for (int i = 0; i < antColonyConfig.NUM_ANTS(); i++) {
      ants.add(new Ant(ctx.plgNetwork, graph, antColonyConfig));
    }

    Routes bestSolution = null;
    int numThreads = Math.min(ants.size(), Runtime.getRuntime().availableProcessors());
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);

    for (int i = 0; i < antColonyConfig.NUM_ITERATIONS(); i++) {
      ants.forEach(Ant::resetState);
      List<Routes> solutions = Collections.synchronizedList(new ArrayList<>());
      List<Future<Routes>> futures = new ArrayList<>();

      for (Ant ant : ants) {
        Callable<Routes> task = () -> {
          Routes routes = ant.findSolution();
          if (verbose) {
            System.out.println(Thread.currentThread().getName() + " found solution with cost: " + routes.getCost());
          }
          return routes;
        };
        futures.add(executor.submit(task));
      }

      for (Future<Routes> future : futures) {
        try {
          solutions.add(future.get());
        } catch (InterruptedException | ExecutionException e) {
          Thread.currentThread().interrupt();
          System.err.println("Error retrieving ant solution: " + e.getMessage());
        }
      }

      List<Routes> sortedSolutions = new ArrayList<>(solutions);
      sortedSolutions.sort((r1, r2) -> Double.compare(r1.getCost(), r2.getCost()));

      if (!sortedSolutions.isEmpty()) {
        if (bestSolution == null || sortedSolutions.get(0).getCost() < bestSolution.getCost()) {
          bestSolution = sortedSolutions.get(0);
        }
        graph.updatePheromoneMap(sortedSolutions, antColonyConfig);
      }

      if (verbose) {
        System.out.println("Iteration " + i + " completed. Best cost so far: "
            + (bestSolution != null ? bestSolution.getCost() : "N/A"));
      }
    }
    executor.shutdown();
    try {
      if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    assert bestSolution != null;
    return new OptimizerResult(
        bestSolution,
        bestSolution.getCost());
  }

  @Override
  public OptimizerResult run(OptimizerContext ctx, Duration maxDuration) {
    return run(ctx, maxDuration, false);
  }

}
