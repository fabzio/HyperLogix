package com.hyperlogix.server.optimizer.Genetic;

import com.hyperlogix.server.domain.*;
import com.hyperlogix.server.optimizer.AntColony.Ant;
import com.hyperlogix.server.optimizer.AntColony.AntColonyConfig;
import com.hyperlogix.server.optimizer.Graph;
import com.hyperlogix.server.optimizer.Optimizer;
import com.hyperlogix.server.optimizer.OptimizerContext;
import com.hyperlogix.server.optimizer.OptimizerResult;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.time.Duration;
import java.time.LocalDateTime;

public class GeneticOptimizer implements Optimizer {
  private final GeneticConfig config;
  private final ThreadLocal<Random> threadLocalRandom = ThreadLocal.withInitial(Random::new);
  
  public GeneticOptimizer(GeneticConfig geneticConfig) {
    this.config = geneticConfig;
  }

  @Override
  public OptimizerResult run(OptimizerContext context, Duration timeLimit, boolean verbose) {
    final PLGNetwork network = context.plgNetwork;
    final LocalDateTime startTime = context.algorithmStartDate;
    int eliteSelection = (int) (config.POPULATION_SIZE() * config.ELITISM_RATE());
    final AntColonyConfig antColonyConfig = new AntColonyConfig(0, 0, 1, 2, 0, 0, 100);
    
    ThreadLocal<Graph> threadLocalGraph = ThreadLocal.withInitial(() -> 
        new Graph(network, startTime, antColonyConfig));
    
    ThreadLocal<Ant> threadLocalAnt = ThreadLocal.withInitial(() -> {
        Graph graph = threadLocalGraph.get();
        return new Ant(network, graph, antColonyConfig);
    });
    
    AtomicReference<Chromosome> bestOverallRef = new AtomicReference<>(null);
    
    // Usar un contenedor para la población que es final pero su contenido puede cambiar
    final AtomicReference<List<Chromosome>> populationRef = new AtomicReference<>(
        initializePopulation(network, startTime, threadLocalGraph, threadLocalAnt)
    );
    
    int threadsToUse = Math.min(Runtime.getRuntime().availableProcessors(), 4);
    ExecutorService executorService = Executors.newFixedThreadPool(threadsToUse);
    
    try {
        List<Chromosome> elite = Collections.synchronizedList(new ArrayList<>());
        List<Chromosome> population = populationRef.get();
        
        if (!population.isEmpty()) {
            synchronized (population) {
                population.sort(Comparator.comparingDouble(Chromosome::getFitness));
                for (int i = 0; i < eliteSelection && i < population.size(); i++) {
                    elite.add(population.get(i).clone());
                }
            }
        }
        
        for (int gen = 0; gen < config.NUM_GENERATIONS(); gen++) {
            population = populationRef.get();
            if (population.isEmpty()) {
                System.err.println("Warning: Population became empty during generation " + gen);
                break;
            }
            
            List<Chromosome> newPopulation = Collections.synchronizedList(new ArrayList<>());
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < config.POPULATION_SIZE(); i++) {
                futures.add(executorService.submit(() -> {
                    Random random = threadLocalRandom.get();
                    Graph graph = threadLocalGraph.get();
                    Ant ant = threadLocalAnt.get();
                    List<Chromosome> currentPop = populationRef.get();
                    
                    Chromosome parent1, parent2;
                    synchronized (currentPop) {
                        parent1 = tournamentSelection(currentPop, random);
                        parent2 = tournamentSelection(currentPop, random);
                    }
                    
                    Chromosome[] children = crossover(parent1, parent2);
                    
                    Chromosome bestOverall = bestOverallRef.get();
                    mutate(children[0], bestOverall, random);
                    
                    Graph childGraph = graph.clone();
                    childGraph.setPheromoneMap(children[0].getSeed());
                    ant.setGraph(childGraph);
                    children[0].recalculateFitness(ant);
                    ant.resetState();
                    
                    synchronized (newPopulation) {
                        newPopulation.add(children[0]);
                    }
                }));
            }
            
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
            
            synchronized (newPopulation) {
                newPopulation.sort(Comparator.comparingDouble(Chromosome::getFitness));
                
                for (int i = 0; i < eliteSelection && i < elite.size(); i++) {
                    if (i < newPopulation.size()) {
                        newPopulation.set(newPopulation.size() - 1 - i, elite.get(i).clone());
                    }
                }
                
                newPopulation.sort(Comparator.comparingDouble(Chromosome::getFitness));
                
                // Actualizar la referencia a la población en lugar de reasignar la variable
                populationRef.set(new ArrayList<>(newPopulation));
                
                elite.clear();
                List<Chromosome> updatedPopulation = populationRef.get();
                for (int i = 0; i < eliteSelection && i < updatedPopulation.size(); i++) {
                    elite.add(updatedPopulation.get(i).clone());
                }
                
                if (!updatedPopulation.isEmpty()) {
                    Chromosome currentBest = updatedPopulation.get(0);
                    Chromosome previous = bestOverallRef.get();
                    if (previous == null || currentBest.getFitness() < previous.getFitness()) {
                        bestOverallRef.set(currentBest.clone());
                    }
                }
            }
            
            population = populationRef.get();
            if (verbose && !population.isEmpty()) {
                System.out.println("Generation " + gen + " best cost: " + population.get(0).getFitness());
            } else if (!population.isEmpty()) {
                System.out.println("Generation " + gen + " - Best cost: " + population.get(0).getFitness());
            } else {
                System.out.println("Generation " + gen + " - No valid solutions found yet.");
            }
        }
    } finally {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    Chromosome bestOverall = bestOverallRef.get();
    List<Chromosome> finalPopulation = populationRef.get();
    
    if (bestOverall == null && !finalPopulation.isEmpty()) {
        bestOverall = finalPopulation.get(0);
    }
    
    if (bestOverall == null) {
        return new OptimizerResult(null, Double.MAX_VALUE);
    }
    
    return new OptimizerResult(bestOverall.getRoutes(), bestOverall.getFitness());
  }

  private List<Chromosome> initializePopulation(
      PLGNetwork network, 
      LocalDateTime startTime, 
      ThreadLocal<Graph> threadLocalGraph,
      ThreadLocal<Ant> threadLocalAnt) {
    
    List<Chromosome> population = Collections.synchronizedList(new ArrayList<>());
    int threadsToUse = Math.min(Runtime.getRuntime().availableProcessors(), 4);
    ExecutorService executorService = Executors.newFixedThreadPool(threadsToUse);
    
    try {
        List<Future<?>> futures = new ArrayList<>();
        
        for (int i = 0; i < config.POPULATION_SIZE(); i++) {
            futures.add(executorService.submit(() -> {
                Graph graph = threadLocalGraph.get().clone();
                Ant ant = threadLocalAnt.get();
                Random random = threadLocalRandom.get();
                
                Map<Node, Map<Node, Double>> seedMap = graph.getPheromoneMap();
                for (Node origin : seedMap.keySet()) {
                    for (Node destination : seedMap.get(origin).keySet()) {
                        seedMap.get(origin).put(destination, 
                            seedMap.get(origin).get(destination) * random.nextDouble());
                    }
                }
                
                ant.setGraph(graph);
                Routes routes = ant.findSolution();
                ant.resetState();
                
                Chromosome chromosome = new Chromosome(
                    new HashMap<>(seedMap),
                    routes, 
                    routes.getCost()
                );
                
                synchronized (population) {
                    population.add(chromosome);
                }
            }));
        }
        
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    } finally {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    return population;
  }

  private Chromosome tournamentSelection(List<Chromosome> population, Random random) {
    List<Chromosome> tournament = new ArrayList<>();
    
    for (int i = 0; i < config.TOURNAMENT_SIZE(); i++) {
        tournament.add(population.get(random.nextInt(population.size())));
    }
    
    return Collections.min(tournament, Comparator.comparingDouble(Chromosome::getFitness));
  }

  private Chromosome[] crossover(Chromosome parent1, Chromosome parent2) {
    Chromosome best = parent1.getFitness() < parent2.getFitness() ? parent1 : parent2;
    Chromosome worst = parent1.getFitness() < parent2.getFitness() ? parent2 : parent1;
    
    Chromosome child = best.clone();

    Map<Node, Map<Node, Double>> bestSeed = best.getSeed();
    Routes bestRoutes = best.getRoutes();

    Map<Node, Map<Node, Double>> worstSeed = worst.getSeed();
    Routes worstRoutes = worst.getRoutes();

    for (String truck : bestRoutes.getRoutes().keySet()) {
        List<Stop> route = bestRoutes.getRoutes().get(truck);
        for (int i = 0; i < route.size() - 1; i++) {
            Node origin = route.get(i).getNode();
            Node destination = route.get(i + 1).getNode();
            
            if (bestSeed.containsKey(origin) && bestSeed.get(origin).containsKey(destination) &&
                worstSeed.containsKey(origin) && worstSeed.get(origin).containsKey(destination)) {
                
                double difference = bestSeed.get(origin).get(destination) - worstSeed.get(origin).get(destination);
                double currentValue = child.getSeed().get(origin).getOrDefault(destination, 0.0);
                double updated = currentValue + difference * config.CROSSOVER_RATE();
                
                child.getSeed().computeIfAbsent(origin, k -> new HashMap<>()).put(destination, updated);
            }
        }
    }

    for (String truck : worstRoutes.getRoutes().keySet()) {
        List<Stop> route = worstRoutes.getRoutes().get(truck);
        for (int i = 0; i < route.size() - 1; i++) {
            Node origin = route.get(i).getNode();
            Node destination = route.get(i + 1).getNode();
            
            if (bestSeed.containsKey(origin) && bestSeed.get(origin).containsKey(destination) &&
                worstSeed.containsKey(origin) && worstSeed.get(origin).containsKey(destination)) {
                
                double pheromone = bestSeed.get(origin).get(destination) - worstSeed.get(origin).get(destination);
                double currentValue = child.getSeed().get(origin).getOrDefault(destination, 0.0);
                double updated = currentValue + pheromone * config.CROSSOVER_RATE();
                
                child.getSeed().computeIfAbsent(origin, k -> new HashMap<>()).put(destination, updated);
            }
        }
    }

    return new Chromosome[] { child };
  }

  private void mutate(Chromosome chromosome, Chromosome bestOverall, Random random) {
    Routes routes = chromosome.getRoutes();
    
    for (String truck : routes.getRoutes().keySet()) {
        List<Stop> route = routes.getRoutes().get(truck);
        for (int i = 0; i < route.size() - 1; i++) {
            Node origin = route.get(i).getNode();
            Node destination = route.get(i + 1).getNode();
            
            if (chromosome.getSeed().containsKey(origin) && 
                chromosome.getSeed().get(origin).containsKey(destination)) {
                
                double probability = chromosome.getSeed().get(origin).get(destination);
                
                if (bestOverall == null) {
                    double updated = probability + (random.nextDouble() - 0.5) * config.MUTATION_RATE();
                    chromosome.getSeed().get(origin).put(destination, updated);
                } else if (bestOverall.getSeed().containsKey(origin) && 
                           bestOverall.getSeed().get(origin).containsKey(destination)) {
                    double differenceWithBest = bestOverall.getSeed().get(origin).get(destination) - probability;
                    double updated = probability + differenceWithBest * config.MUTATION_RATE();
                    chromosome.getSeed().get(origin).put(destination, updated);
                }
            }
        }
    }
  }
}