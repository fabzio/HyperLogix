package com.hyperlogix.server.optimizer.AntColony;

import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.optimizer.Optimizer;
import com.hyperlogix.server.optimizer.OptimizerContext;
import com.hyperlogix.server.optimizer.OptimizerResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

    for (int i = 0; i < antColonyConfig.NUM_ITERATIONS(); i++) {
      ants.forEach(Ant::resetState);
      List<Routes> solutions = new ArrayList<>();
      for (Ant ant : ants) {
        Routes routes = ant.findSolution();
        solutions.add(routes);
        if (verbose) {
          System.out.println("Ant found solution with cost: " + routes.getCost());
        }
      }

      solutions.sort((r1, r2) -> Double.compare(r1.getCost(), r2.getCost()));

      if (bestSolution == null || solutions.get(0).getCost() < bestSolution.getCost()) {
        bestSolution = solutions.get(0);
      }

      graph.updatePheromoneMap(solutions, antColonyConfig);

      if (verbose) {
        System.out.println("Iteration " + i + " completed.");
      }
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
