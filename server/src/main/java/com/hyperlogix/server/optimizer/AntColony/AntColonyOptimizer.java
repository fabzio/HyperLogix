package com.hyperlogix.server.optimizer.AntColony;

import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.optimizer.Graph;
import com.hyperlogix.server.optimizer.Notifier;
import com.hyperlogix.server.optimizer.Optimizer;
import com.hyperlogix.server.optimizer.OptimizerContext;
import com.hyperlogix.server.optimizer.OptimizerResult;
import com.hyperlogix.server.features.planification.dtos.LogisticCollapseEvent;

import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class AntColonyOptimizer implements Optimizer {
  private AntColonyConfig antColonyConfig;
  private Graph graph;
  private List<Ant> ants;
  private ApplicationEventPublisher eventPublisher;
  private String sessionId;

  public AntColonyOptimizer(AntColonyConfig antColonyConfig) {
    this.antColonyConfig = antColonyConfig;
  }

  public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
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
    ExecutorService executor = Executors.newFixedThreadPool(numThreads, r -> {
      Thread t = new Thread(r, "AntColony-" + System.currentTimeMillis());
      t.setDaemon(true);
      return t;
    });

    long startTime = System.currentTimeMillis();
    long maxDurationMillis = maxDuration.toMillis();

    try {
      for (int i = 0; i < antColonyConfig.NUM_ITERATIONS(); i++) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime >= maxDurationMillis) {
          System.out.println("Optimization terminated due to time limit. Completed " + i + " iterations.");
          break;
        }

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
    } finally {
      // Ensure executor is properly shut down
      executor.shutdown();
      try {
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          executor.shutdownNow();
          if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            System.err.println("AntColonyOptimizer: Executor did not terminate after forced shutdown");
          }
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }

    // Si no se encontró solución válida dentro del tiempo límite, publicar
    // LogisticCollapse
    if (bestSolution == null && eventPublisher != null && sessionId != null) {
      LogisticCollapseEvent collapseEvent = new LogisticCollapseEvent(
          sessionId,
          "TIME_LIMIT_EXCEEDED",
          "No se pudo encontrar una solución válida dentro del tiempo límite de " + maxDuration.toMinutes()
              + " minutos",
          java.time.LocalDateTime.now(),
          0.95,
          "Algoritmo de optimización");
      eventPublisher.publishEvent(collapseEvent);
    }

    // Return best solution found
    if (bestSolution != null) {
      return new OptimizerResult(bestSolution, bestSolution.getCost());
    } else {
      return new OptimizerResult(null, Double.MAX_VALUE);
    }
  }

  @Override
  public OptimizerResult run(OptimizerContext ctx, Duration maxDuration) {
    return run(ctx, maxDuration, null);
  }

}
