package com.hyperlogix.server.optimizer.Genetic;

import com.hyperlogix.server.domain.Path;
import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.domain.Stop;
import com.hyperlogix.server.optimizer.Optimizer;
import com.hyperlogix.server.optimizer.OptimizerContext;
import com.hyperlogix.server.optimizer.OptimizerResult;
import com.hyperlogix.server.optimizer.AntColony.Ant;
import com.hyperlogix.server.optimizer.AntColony.Graph;
import com.hyperlogix.server.domain.PLGNetwork;
import java.util.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import com.hyperlogix.server.optimizer.AntColony.AntColonyConfig;

public class GeneticOptimizer implements Optimizer {
  private final GeneticConfig config;
  private final Random random = new Random();

  public GeneticOptimizer(GeneticConfig geneticConfig) {
    this.config = geneticConfig;
  }

  @Override
  public OptimizerResult run(OptimizerContext context, Duration timeLimit, boolean verbose) {
    final PLGNetwork network = context.plgNetwork;
    final LocalDateTime startTime = context.algorithmStartDate;
    List<Chromosome> population = initializePopulation(network, startTime);
    Chromosome best = null;
    if (!population.isEmpty()) {
      best = Collections.min(population, Comparator.comparingDouble(Chromosome::getFitness));
    }
    for (int gen = 0; gen < config.NUM_GENERATIONS(); gen++) {
      List<Chromosome> newPopulation = new ArrayList<>();
      if (population.isEmpty()) {
        System.err.println("Warning: Population became empty during generation " + gen);
        break;
      }
      while (newPopulation.size() < config.POPULATION_SIZE()) {
        Chromosome parent1 = tournamentSelection(population);
        Chromosome parent2 = tournamentSelection(population);
        Chromosome child = crossover(parent1, parent2);
        mutate(child, network, startTime);
        newPopulation.add(child);
      }
      population = newPopulation;
      if (!population.isEmpty()) {
        Chromosome genBest = Collections.min(population, Comparator.comparingDouble(Chromosome::getFitness));
        if (best == null || genBest.getFitness() < best.getFitness()) {
          best = genBest;
        }
      }
      if (verbose && best != null) {
        System.out.println("Generation " + gen + " best cost: " + best.getFitness());
      } else if (verbose) {
        System.out.println("Generation " + gen + " - No valid solutions found yet.");
      }
    }
    if (best == null) {
      return new OptimizerResult(new Routes(new HashMap<>(), new HashMap<>(), Double.POSITIVE_INFINITY),
          Double.POSITIVE_INFINITY);
    }
    return new OptimizerResult(best.getRoutes(), best.getFitness());
  }

  private List<Chromosome> initializePopulation(PLGNetwork network, LocalDateTime startTime) {
    List<Chromosome> population = new ArrayList<>();
    AntColonyConfig antConfig = new AntColonyConfig(1, 1, 0.9, 0.3, 1.5, 0.05, 2.0);
    for (int i = 0; i < config.POPULATION_SIZE(); i++) {
      PLGNetwork clonedNetwork = network.clone();
      Graph graph = new Graph(clonedNetwork, startTime, antConfig);
      Ant ant = new Ant(clonedNetwork, graph, antConfig);
      Routes initialRoutes = ant.findSolution();
      if (initialRoutes != null && initialRoutes.getCost() != Double.POSITIVE_INFINITY) {
        population.add(new Chromosome(initialRoutes));
      }
    }
    if (population.isEmpty()) {
      System.err.println("Warning: Initial population is empty. No valid routes found by ants.");
    }
    return population;
  }

  private Chromosome tournamentSelection(List<Chromosome> population) {
    if (population.isEmpty()) {
      throw new IllegalStateException("Cannot perform selection on an empty population.");
    }
    List<Chromosome> tournament = new ArrayList<>();
    for (int i = 0; i < config.TOURNAMENT_SIZE(); i++) {
      tournament.add(population.get(random.nextInt(population.size())));
    }
    return Collections.min(tournament, Comparator.comparingDouble(Chromosome::getFitness));
  }

  private Chromosome crossover(Chromosome parent1, Chromosome parent2) {
    Map<String, List<Stop>> childRoutesMap = new HashMap<>();
    Map<String, List<Path>> childPathsMap = new HashMap<>();
    Set<String> truckIds = new HashSet<>();
    if (parent1.getRoutes() != null && parent1.getRoutes().getRoutes() != null)
      truckIds.addAll(parent1.getRoutes().getRoutes().keySet());
    if (parent2.getRoutes() != null && parent2.getRoutes().getRoutes() != null)
      truckIds.addAll(parent2.getRoutes().getRoutes().keySet());
    for (String truckId : truckIds) {
      if (random.nextBoolean()) {
        copyRouteFromParent(parent1, childRoutesMap, childPathsMap, truckId);
      } else {
        copyRouteFromParent(parent2, childRoutesMap, childPathsMap, truckId);
      }
    }
    double cost1 = (parent1.getRoutes() != null) ? parent1.getRoutes().getCost() : Double.POSITIVE_INFINITY;
    double cost2 = (parent2.getRoutes() != null) ? parent2.getRoutes().getCost() : Double.POSITIVE_INFINITY;
    double childCost = Math.min(cost1, cost2);
    Routes childRoutes = new Routes(childRoutesMap, childPathsMap, childCost);
    return new Chromosome(childRoutes);
  }

  private void copyRouteFromParent(Chromosome parent, Map<String, List<Stop>> childRoutes,
      Map<String, List<Path>> childPaths, String truckId) {
    if (parent.getRoutes() == null)
      return;
    if (parent.getRoutes().getRoutes() != null && parent.getRoutes().getRoutes().containsKey(truckId)) {
      childRoutes.put(truckId, new ArrayList<>(parent.getRoutes().getRoutes().get(truckId)));
    }
    if (parent.getRoutes().getPaths() != null && parent.getRoutes().getPaths().containsKey(truckId)) {
      childPaths.put(truckId, new ArrayList<>(parent.getRoutes().getPaths().get(truckId)));
    }
  }

  private void mutate(Chromosome chromosome, PLGNetwork network, LocalDateTime startTime) {
    if (random.nextDouble() < config.MUTATION_RATE()) {
      Routes routes = chromosome.getRoutes();
      if (routes == null || routes.getRoutes() == null || routes.getRoutes().isEmpty()) {
        return;
      }
      List<String> eligibleTrucks = routes.getRoutes().entrySet().stream()
          .filter(entry -> entry.getValue() != null && entry.getValue().size() >= 2)
          .map(Map.Entry::getKey)
          .collect(Collectors.toList());
      if (eligibleTrucks.isEmpty()) {
        return;
      }
      String truckIdToMutate = eligibleTrucks.get(random.nextInt(eligibleTrucks.size()));
      List<Stop> routeToMutate = routes.getRoutes().get(truckIdToMutate);
      int index1 = random.nextInt(routeToMutate.size());
      int index2 = random.nextInt(routeToMutate.size());
      while (index1 == index2) {
        index2 = random.nextInt(routeToMutate.size());
      }
      Collections.swap(routeToMutate, index1, index2);
      chromosome.recalculateFitness(network, startTime);
    }
  }
}
