package com.hyperlogix.server.optimizer.AntColony;

import com.hyperlogix.server.config.Constants;
import com.hyperlogix.server.domain.*;
import com.hyperlogix.server.optimizer.Graph;
import com.hyperlogix.server.features.planification.dtos.LogisticCollapseEvent;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class Ant {
  private PLGNetwork network;
  private final PLGNetwork originalNetwork;
  @Getter
  @Setter
  private Graph graph;
  private final AntColonyConfig antColonyConfig;
  private List<Node> nodesLeft;
  private Map<Node, Map<Node, Path>> adjacencyMap;
  private Map<String, List<Stop>> routes;
  private Map<String, List<Path>> paths;
  private Map<String, Duration> tourTime;
  private Map<String, Double> tourCost;
  private Map<Node, Path> firstPath;
  private ApplicationEventPublisher eventPublisher;
  private String sessionId;

  public Ant(PLGNetwork network, Graph graph, AntColonyConfig antColonyConfig) {
    this.originalNetwork = network.clone();
    this.graph = graph;
    this.antColonyConfig = antColonyConfig;

    network.getTrucksCapacity();
    resetState();
  }

  public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public Routes findSolution() {
    for (Truck truck : network.getTrucks()) {
      // Create the first node representing the truck's starting location
      Stop firstNode = new Stop(
          new Node(truck.getCode(), truck.getType().toString(), NodeType.LOCATION, truck.getLocation().integerPoint()),
          graph.getAlgorithmStartDate());
      routes.put(truck.getId(), new ArrayList<>(List.of(firstNode)));
      List<Stop> availableNodes = getAvailableNodes(truck, firstNode);
      if (availableNodes.isEmpty()) {
        continue;
      }
      Stop nextNode = availableNodes.get(new Random().nextInt(availableNodes.size()));
      moveToNode(truck, firstNode, nextNode);
    }

    while (!nodesLeft.stream().filter(node -> node.getType() == NodeType.DELIVERY).toList().isEmpty()) {
      // Select best truck instead of iterating in fixed order
      Truck bestTruck = selectBestTruck();
      if (bestTruck == null) {
        System.out.println("Logistic collapse, no more trucks available");

        // Publicar evento de colapso logístico
        if (eventPublisher != null && sessionId != null) {
          LogisticCollapseEvent collapseEvent = new LogisticCollapseEvent(
            sessionId,
            "RESOURCE_SHORTAGE",
            "No hay más camiones disponibles para completar las entregas pendientes",
            LocalDateTime.now(),
            0.9,
            "Flota de vehículos"
          );
          eventPublisher.publishEvent(collapseEvent);
        }

        // Process the current routes with A* before returning
        Routes roughSolution = new Routes(routes, paths,
            tourCost.values().stream().mapToDouble(Double::doubleValue).sum());
        return graph.processRoutesWithAStar(roughSolution, graph.getAlgorithmStartDate());
      }

      Stop currentNode = routes.get(bestTruck.getId()).getLast();
      Stop nextNode = getNextNode(currentNode, bestTruck);
      if (nextNode == null) {
        // Mark truck as temporarily unavailable by setting a flag or continue
        continue;
      }
      moveToNode(bestTruck, currentNode, nextNode);
    }

    // Process the final routes with A* to get exact paths and timing
    Routes roughSolution = new Routes(routes, paths, tourCost.values().stream().mapToDouble(Double::doubleValue).sum());
    return graph.processRoutesWithAStar(roughSolution, graph.getAlgorithmStartDate());
  }

  private Truck selectBestTruck() {
    List<Truck> availableTrucks = new ArrayList<>();
    List<Double> truckScores = new ArrayList<>();

    for (Truck truck : network.getTrucks()) {
      if (truck.getStatus() == TruckState.MAINTENANCE || truck.getStatus() == TruckState.BROKEN_DOWN
          || truck.getStatus() == TruckState.ACTIVE) {
        continue;
      }

      Stop currentNode = routes.get(truck.getId()).getLast();
      List<Stop> availableNodes = getAvailableNodes(truck, currentNode);

      if (availableNodes.isEmpty()) {
        continue;
      }

      availableTrucks.add(truck);

      // Calculate truck efficiency score
      double score = calculateTruckScore(truck, currentNode, availableNodes);
      truckScores.add(score);
    }

    if (availableTrucks.isEmpty()) {
      return null;
    }

    // Select truck using weighted random selection based on scores
    double totalScore = truckScores.stream().mapToDouble(Double::doubleValue).sum();
    if (totalScore == 0) {
      return availableTrucks.get(new Random().nextInt(availableTrucks.size()));
    }

    List<Double> probabilities = truckScores.stream().map(score -> score / totalScore).toList();

    double randomValue = new Random().nextDouble();
    double cumulativeProbability = 0.0;
    for (int i = 0; i < availableTrucks.size(); i++) {
      cumulativeProbability += probabilities.get(i);
      if (randomValue <= cumulativeProbability) {
        return availableTrucks.get(i);
      }
    }
    return availableTrucks.getLast();
  }

  private double calculateTruckScore(Truck truck, Stop currentNode, List<Stop> availableNodes) {
    // Base efficiency: smaller trucks are more fuel efficient
    double fuelEfficiency = switch (truck.getType()) {
      case TD -> 4.0; // Best fuel efficiency (smallest truck)
      case TC -> 3.0;
      case TB -> 2.0;
      case TA -> 1.0; // Worst fuel efficiency (largest truck)
    };

    // Capacity utilization factor - prefer trucks that can make full use of their
    // capacity
    double remainingOrders = nodesLeft.stream()
        .filter(node -> node.getType() == NodeType.DELIVERY)
        .mapToDouble(node -> {
          Order order = network.getOrders().stream()
              .filter(o -> o.getId().equals(node.getId()))
              .findFirst().orElse(null);
          return order != null ? (order.getRequestedGLP() - order.getDeliveredGLP()) : 0;
        }).sum();

    double capacityMatch = Math.min(truck.getCurrentCapacity() / Math.max(remainingOrders, 1.0), 1.0);

    // Status bonus - prefer IDLE trucks to better distribute work
    double statusBonus = switch (truck.getStatus()) {
      case IDLE -> 2.0;
      case ACTIVE -> 0.5;
      default -> 0.1;
    };

    // Distance factor - consider proximity to available nodes
    double avgDistance = availableNodes.stream()
        .mapToDouble(node -> {
          if (currentNode.getNode().getType() == NodeType.LOCATION) {
            return firstPath.containsKey(node.getNode()) ? firstPath.get(node.getNode()).length() : 1000; // Fallback
                                                                                                          // high value
          } else {
            return adjacencyMap.get(currentNode.getNode()).get(node.getNode()).length();
          }
        })
        .average().orElse(1000.0);

    double distanceFactor = 1000.0 / Math.max(avgDistance, 1.0);

    // Fuel level factor - trucks with more fuel are preferred
    double fuelFactor = truck.getCurrentFuel() / truck.getFuelCapacity();

    // Combine factors
    return fuelEfficiency * statusBonus * capacityMatch * distanceFactor * fuelFactor;
  }

  private List<Stop> getAvailableNodes(Truck truck, Stop currentNode) {
    List<Stop> availableNodes = new ArrayList<>();
    if (truck.getStatus() == TruckState.MAINTENANCE || truck.getStatus() == TruckState.BROKEN_DOWN) {
      return List.of();
    }
    for (Node node : nodesLeft) {
      if (node.getId().equals(currentNode.getNode().getId()))
        continue;
      int distance;
      if (currentNode.getNode().getType() == NodeType.LOCATION) {
        // Use Manhattan distance instead of A*
        distance = calculateManhattanDistance(currentNode.getNode().getLocation(), node.getLocation());
        firstPath.put(node, new Path(List.of(currentNode.getNode().getLocation(), node.getLocation()), distance));

      } else
        distance = adjacencyMap.get(currentNode.getNode()).get(node).length();
      Duration timeToDestination = truck.getTimeToDestination(distance);
      LocalDateTime arrivalTime = currentNode.getArrivalTime().plus(timeToDestination);
      double fuelConsumption = truck.getFuelConsumption(distance);
      if (truck.getCurrentFuel() < fuelConsumption)
        continue;
      if (node.getType() == NodeType.STATION) {
        Station station = network.getStations().stream()
            .filter(s -> s.getId().equals(node.getId()))
            .findFirst().orElse(null);
        assert station != null;
        int refillableCapacity = Math.min(truck.getMaxCapacity() - truck.getCurrentCapacity(),
            station.getAvailableCapacity(arrivalTime));

        int glpToFull = truck.getMaxCapacity() - truck.getCurrentCapacity();
        if (glpToFull < truck.getMaxCapacity() * 0.3 && refillableCapacity <= glpToFull * 0.3
            && truck.getCurrentFuel() > 0.3 * truck.getFuelCapacity())
          continue;
      } else if (node.getType() == NodeType.DELIVERY) {
        Order order = network.getOrders().stream().filter(o -> o.getId().equals(node.getId()))
            .findFirst().orElse(null);
        assert order != null;
        if ((currentNode.getArrivalTime().plus(timeToDestination).isAfter(order.getMaxDeliveryDate())))
          continue;
        // ||
        // currentNode.getArrivalTime().plus(timeToDestination).isBefore(order.getMinDeliveryDate()))
        double fuelAfterDelivery = truck.getCurrentFuel() - fuelConsumption;
        double fuelToNearestStation = adjacencyMap.get(node).entrySet().stream()
            .filter(entry -> entry.getKey().getType() == NodeType.STATION)
            .map(entry -> entry.getValue().length()).mapToDouble(truck::getFuelConsumption).min()
            .orElse(Double.POSITIVE_INFINITY);
        if (fuelToNearestStation > fuelAfterDelivery)
          continue;
      }
      availableNodes.add(new Stop(node, arrivalTime));
    }
    return availableNodes;
  }

  private int calculateManhattanDistance(Point from, Point to) {
    return (int) ((Math.abs(from.x() - to.x()) + Math.abs(from.y() - to.y())) * Constants.EDGE_LENGTH);
  }

  private Stop getNextNode(Stop currentNode, Truck truck) {
    List<Stop> availableNodes = getAvailableNodes(truck, currentNode);
    if (availableNodes.isEmpty()) {
      return null;
    }
    List<Double> scores = new ArrayList<>();
    for (Stop node : availableNodes) {
      int distance = adjacencyMap.get(currentNode.getNode()).get(node.getNode()).length();
      double pheromone = graph.getPheromoneMap().get(currentNode.getNode()).get(node.getNode());

      double penalization;
      if (node.getNode().getType() == NodeType.STATION) {
        double capacityFactor = 1 + (double) truck.getCurrentCapacity() / truck.getMaxCapacity();
        penalization = capacityFactor;
      } else {

        // Suponemos que puedes acceder a la orden por ID
        Order order = network.getOrders().stream()
            .filter(o -> o.getId().equals(node.getNode().getId()))
            .findFirst().orElse(null);
        // Usar la urgencia basada en la ventana de entrega
        Duration timeLeft = Duration.between(currentNode.getArrivalTime(), order.getMaxDeliveryDate());
        long minutesLeft = timeLeft.toMinutes();

        long maxTimeLeft = network.getOrders().stream()
            .filter(o -> o.getMaxDeliveryDate().isAfter(currentNode.getArrivalTime()))
            .mapToLong(o -> Duration.between(currentNode.getArrivalTime(), o.getMaxDeliveryDate()).toMinutes())
            .max().orElse(1);

        double urgencyFactor = 1 - Math.min((double) minutesLeft / maxTimeLeft, 1.0); // Más cerca del deadline = mayor
                                                                                      // urgencia
        double urgencyPenaltyScale = 0.5;
        double urgencyPenalty = 1 + urgencyPenaltyScale * urgencyFactor;

        penalization = urgencyPenalty / (1 + (double) truck.getCurrentCapacity() / truck.getMaxCapacity());

      }

      double heuristic = 1.0 / (penalization * distance);
      double score = Math.pow(pheromone, antColonyConfig.ALPHA()) * Math.pow(heuristic, antColonyConfig.BETA());
      scores.add(score);
    }

    double totalScore = scores.stream().mapToDouble(Double::doubleValue).sum();
    List<Double> probabilities = scores.stream().map(score -> score / totalScore).toList();

    double randomValue = new Random().nextDouble();
    double cumulativeProbability = 0.0;
    for (int i = 0; i < availableNodes.size(); i++) {
      cumulativeProbability += probabilities.get(i);
      if (randomValue <= cumulativeProbability) {
        return availableNodes.get(i);
      }
    }
    return availableNodes.getLast();
  }
  private void moveToNode(Truck truck, Stop currentNode, Stop nextNode) {
    Path path;
    if (currentNode.getNode().getType() == NodeType.LOCATION) {
      path = firstPath.get(nextNode.getNode());
    } else {
      path = adjacencyMap.get(currentNode.getNode()).get(nextNode.getNode());
    }
    
    // Normal movement logic when no incident or incident doesn't affect movement
    this.paths.get(truck.getId())
        .add(path.points().getFirst() == currentNode.getNode().getLocation() ? path : path.reverse());
    int distance = path.length();
    // No need to recalculate adjacency map since we're using Manhattan distance
    Duration timeToDestination = truck.getTimeToDestination(distance);
    double fuelConsumption = truck.getFuelConsumption(distance);
    nextNode.setArrivalTime(currentNode.getArrivalTime().plus(timeToDestination));
    this.routes.get(truck.getId()).add(nextNode);

    if (nextNode.getNode().getType() == NodeType.STATION) {
      truck.setCurrentFuel(truck.getFuelCapacity());
      int glpToFull = truck.getMaxCapacity() - truck.getCurrentCapacity();
      Station station = network.getStations().stream()
          .filter(s -> s.getId().equals(nextNode.getNode().getId()))
          .findFirst().orElse(null);
      assert station != null;
      int glpToRefill = Math.min(glpToFull, station.getAvailableCapacity(nextNode.getArrivalTime()));
      station.reserveCapacity(nextNode.getArrivalTime(), glpToRefill, truck.getId(), null);
      truck.setCurrentCapacity(truck.getCurrentCapacity() + glpToRefill);
      truck.setCurrentFuel(truck.getMaxCapacity());
    } else if (nextNode.getNode().getType() == NodeType.DELIVERY) {
      Order order = network.getOrders().stream().filter(o -> o.getId().equals(nextNode.getNode().getId()))
          .findFirst().orElse(null);
      assert order != null;
      int glpToDeliver = Math.min(truck.getCurrentCapacity(), order.getRequestedGLP() - order.getDeliveredGLP());

      if (order.getDeliveredGLP() + glpToDeliver == order.getRequestedGLP()) {
        order.setDeliveredGLP(order.getRequestedGLP());
        nodesLeft.remove(nextNode.getNode());
      } else
        order.setDeliveredGLP(order.getDeliveredGLP() + glpToDeliver);
      truck.setCurrentCapacity(truck.getCurrentCapacity() - glpToDeliver);
      truck.setCurrentFuel(truck.getCurrentFuel() - fuelConsumption);
      truck.setLocation(nextNode.getNode().getLocation());
    }
    this.tourTime.put(truck.getId(), this.tourTime.get(truck.getId()).plus(timeToDestination));
    this.tourCost.put(truck.getId(), this.tourCost.get(truck.getId()) + fuelConsumption);

  }
  public void resetState() {
    this.network = originalNetwork.clone();
    this.adjacencyMap = graph.createAdjacencyMap(graph.getAlgorithmStartDate());
    this.nodesLeft = new ArrayList<>(adjacencyMap.keySet().stream().toList());
    this.routes = network.getTrucks().stream()
        .collect(Collectors.toMap(Truck::getId, truck -> new ArrayList<>())); // Use mutable list
    this.paths = network.getTrucks().stream()
        .collect(Collectors.toMap(Truck::getId, truck -> new ArrayList<>())); // Use mutable list
    this.tourTime = network.getTrucks().stream()
        .collect(Collectors.toMap(Truck::getId, truck -> Duration.ZERO));
    this.tourCost = network.getTrucks().stream()
        .collect(Collectors.toMap(Truck::getId, truck -> 0.0));
    this.firstPath = new HashMap<>();
  }

}
