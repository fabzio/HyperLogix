package com.hyperlogix.server.optimizer.AntColony;

import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.domain.Stop;
import com.hyperlogix.server.domain.Node;
import com.hyperlogix.server.domain.NodeType;
import com.hyperlogix.server.domain.Path;
import com.hyperlogix.server.domain.TruckState;
import com.hyperlogix.server.domain.OrderStatus;
import com.hyperlogix.server.optimizer.Graph;
import com.hyperlogix.server.optimizer.Notifier;
import com.hyperlogix.server.optimizer.Optimizer;
import com.hyperlogix.server.optimizer.OptimizerContext;
import com.hyperlogix.server.optimizer.OptimizerResult;
import com.hyperlogix.server.features.planification.dtos.LogisticCollapseEvent;

import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.*;

public class AntColonyOptimizer implements Optimizer {
  private AntColonyConfig antColonyConfig;
  private Graph graph;
  private List<Ant> ants;
  private ApplicationEventPublisher eventPublisher;
  private String sessionId;
  private OptimizerContext currentContext; // Para usar en la solución de emergencia

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
    this.currentContext = ctx;

    graph = new Graph(ctx.plgNetwork, ctx.algorithmStartDate, antColonyConfig, ctx.incidents);

    ants = new ArrayList<>();
    for (int i = 0; i < antColonyConfig.NUM_ANTS(); i++) {
      Ant ant = new Ant(ctx.plgNetwork, graph, antColonyConfig, ctx.incidents);
      ant.setEventPublisher(eventPublisher);
      ant.setSessionId(sessionId);
      ants.add(ant);
    }

    Routes bestSolution = null;
    long startTime = System.currentTimeMillis();
    long maxDurationMillis = maxDuration.toMillis();

    for (int iteration = 0; iteration < antColonyConfig.NUM_ITERATIONS(); iteration++) {
      long elapsedTime = System.currentTimeMillis() - startTime;

      if (elapsedTime >= maxDurationMillis) {
        System.out.println("Optimization terminated due to time limit. Completed " + iteration + " iterations.");
        break;
      }

      ants.forEach(Ant::resetState);
      List<Routes> solutions = new ArrayList<>();
      for (Ant ant : ants) {
        try {
          Routes result = ant.findSolution();
          if (result != null) {
            solutions.add(result);
          }
        } catch (Exception e) {
          System.err.println("Ant execution error: " + e.getMessage());
          e.printStackTrace();
        }

        elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime >= maxDurationMillis) {
          System.out.println("Time limit reached during sequential ant execution.");
          break;
        }
      }
      if (!solutions.isEmpty()) {
        solutions.sort((r1, r2) -> Double.compare(r1.getCost(), r2.getCost()));

        if (bestSolution == null || solutions.get(0).getCost() < bestSolution.getCost()) {
          bestSolution = solutions.get(0);
        }

        graph.updatePheromoneMap(solutions, antColonyConfig);

        if (bestSolution != null && iteration == 0) {
          System.out.println("Found first valid solution in iteration " + iteration + ", continuing to optimize...");
        }
      } else {
        System.err.println("No valid solutions found in iteration " + iteration);
      }

      if (notifier != null) {
        notifier.notify(new OptimizerResult(
            bestSolution,
            bestSolution != null ? bestSolution.getCost() : Double.MAX_VALUE));
      }
    }

    if (bestSolution == null) {
      if (eventPublisher != null && sessionId != null) {
        LogisticCollapseEvent collapseEvent = new LogisticCollapseEvent(
            sessionId,
            "EMERGENCY_SOLUTION_ACTIVATED",
            "Se activó solución de emergencia debido a falta de solución válida en " + maxDuration.toMinutes()
                + " minutos",
            LocalDateTime.now(),
            0.7,
            "Algoritmo de optimización");
        eventPublisher.publishEvent(collapseEvent);
      }
    }

    return bestSolution != null
        ? new OptimizerResult(bestSolution, bestSolution.getCost())
        : new OptimizerResult(null, Double.MAX_VALUE);
  }

  @Override
  public OptimizerResult run(OptimizerContext ctx, Duration maxDuration) {
    return run(ctx, maxDuration, null);
  }

  /**
   * Publica un evento de colapso logístico cuando ocurre una InterruptedException
   */
  private void publishLogisticCollapseEvent(String collapseType, String description,
      double severityLevel, String affectedArea) {
    if (eventPublisher != null) {
      LogisticCollapseEvent collapseEvent = new LogisticCollapseEvent(
          sessionId != null ? sessionId : "UNKNOWN_SESSION",
          collapseType,
          description,
          LocalDateTime.now(),
          severityLevel,
          affectedArea);

      eventPublisher.publishEvent(collapseEvent);
      System.err.println("COLAPSO LOGÍSTICO DETECTADO: " + collapseType + " - " + description);
    }
  }

}
