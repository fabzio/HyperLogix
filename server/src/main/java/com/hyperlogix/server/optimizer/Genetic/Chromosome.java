package com.hyperlogix.server.optimizer.Genetic;

import com.hyperlogix.server.domain.*;
import com.hyperlogix.server.optimizer.AntColony.Ant;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

@Data
@AllArgsConstructor
public class Chromosome implements Cloneable {
  private Map<Node, Map<Node, Double>> seed;
  private Routes routes;
  private double fitness;

  @Override
  public Chromosome clone() {
    try {
      Chromosome cloned = (Chromosome) super.clone();
      cloned.seed = new HashMap<>();
      for (Map.Entry<Node, Map<Node, Double>> entry : this.seed.entrySet()) {
        cloned.seed.put(entry.getKey(), new HashMap<>(entry.getValue()));
      }
      return cloned;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }

  public void recalculateFitness(Ant antSolver){

    this.routes = antSolver.findSolution();
    this.fitness = routes.getCost();
    antSolver.resetState();
  }
}
