package com.hyperlogix.server.optimizer.AntColony;

import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.optimizer.Graph;
import com.hyperlogix.server.optimizer.Notifier;
import com.hyperlogix.server.optimizer.Optimizer;
import com.hyperlogix.server.optimizer.OptimizerContext;
import com.hyperlogix.server.optimizer.OptimizerResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import com.hyperlogix.server.config.Constants;
import com.hyperlogix.server.domain.OrderStatus;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Path;
import com.hyperlogix.server.domain.Point;
import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.util.AStar;

import ch.qos.logback.core.joran.action.NOPAction;

public class AntColonyOptimizer implements Optimizer {
  private AntColonyConfig antColonyConfig;
  private Graph graph;
  private List<Ant> ants;

  public AntColonyOptimizer(AntColonyConfig antColonyConfig) {
    this.antColonyConfig = antColonyConfig;
  }

  @Override
  public OptimizerResult run(OptimizerContext ctx, Duration maxDuration, Notifier notifier) {
    graph = new Graph(ctx.plgNetwork, ctx.algorithmStartDate, antColonyConfig);

    ants = new ArrayList<>();
    for (int i = 0; i < antColonyConfig.NUM_ANTS(); i++) {
      Ant ant = new Ant(ctx.plgNetwork, graph, antColonyConfig);
      ants.add(ant);
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
          e.printStackTrace();
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

      if (notifier != null) {
        notifier.notify(new OptimizerResult(
            bestSolution,
            bestSolution != null ? bestSolution.getCost() : Double.MAX_VALUE));
      }
    }
    executor.shutdown();
    try {
      if (!executor.awaitTermination(3000, TimeUnit.MILLISECONDS)) {
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
    return run(ctx, maxDuration, null);
  }

}
