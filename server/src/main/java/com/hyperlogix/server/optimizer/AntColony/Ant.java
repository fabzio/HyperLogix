package com.hyperlogix.server.optimizer.AntColony;

import com.hyperlogix.server.config.Constants;
import com.hyperlogix.server.domain.*;
import com.hyperlogix.server.optimizer.Graph;
import com.hyperlogix.server.util.AStar;

import lombok.Getter;
import lombok.Setter;

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

  public Ant(PLGNetwork network, Graph graph, AntColonyConfig antColonyConfig) {
    this.originalNetwork = network.clone();
    this.graph = graph;
    this.antColonyConfig = antColonyConfig;

    network.getTrucksCapacity();
    resetState();
  }

  public Routes findSolution() {
    for (Truck truck : network.getTrucks()) {

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
    int truckWithoutOrdersOrOnlyRefilling = 0;
    while (!nodesLeft.stream().filter(node -> node.getType() == NodeType.DELIVERY).toList().isEmpty()) {
      truckWithoutOrdersOrOnlyRefilling = 0;
      for (Truck truck : network.getTrucks()) {
        Stop currentNode = routes.get(truck.getId()).getLast();
        Stop nextNode = getNextNode(currentNode, truck);
        if (nextNode == null || nextNode.getNode().getType() == NodeType.STATION) {
          truckWithoutOrdersOrOnlyRefilling++;
          if (truckWithoutOrdersOrOnlyRefilling == network.getTrucks().size()) {
            System.out.println("Logistic collapse, no more nodes available");
            truck.setCurrentCapacity(25);
            return new Routes(routes, paths,
                truck.getFuelConsumption((Constants.MAP_HEIGHT + Constants.MAP_WIDTH) * network.getOrders().size()));
          }
          if (nextNode == null)
            continue;
        }
        moveToNode(truck, currentNode, nextNode);
      }
    }
    return new Routes(routes, paths, tourCost.values().stream().mapToDouble(Double::doubleValue).sum());
  }

  private List<Stop> getAvailableNodes(Truck truck, Stop currentNode) {
    List<Stop> availableNodes = new ArrayList<>();
    if (truck.getStatus() == TruckState.MAINTENANCE || truck.getStatus() == TruckState.BROKEN_DOWN) {
      return List.of();
    }

    boolean returningToBase = truck.getStatus() == TruckState.RETURNING_TO_BASE;

    for (Node node : nodesLeft) {
      if (node.getId().equals(currentNode.getNode().getId()))
        continue;
      if (returningToBase && node.getType() != NodeType.STATION)
        continue;
        
      int distance;
      if (currentNode.getNode().getType() == NodeType.LOCATION) {
        // Fallback to A* if not found in cache
        List<Point> res = AStar.encontrarRuta(currentNode.getNode().getLocation(), node.getLocation(),
            currentNode.getArrivalTime(), network.getRoadblocks());
        distance = res.size() * Constants.EDGE_LENGTH;
        firstPath.put(node, new Path(res, distance));

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
    this.paths.get(truck.getId())
        .add(path.points().getFirst() == currentNode.getNode().getLocation() ? path : path.reverse());
    int distance = path.length();
    this.adjacencyMap = graph.createAdjacencyMap(nextNode.getArrivalTime());
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
      station.reserveCapacity(nextNode.getArrivalTime(), glpToRefill);
      truck.setCurrentCapacity(truck.getCurrentCapacity() + glpToRefill);
      truck.setCurrentFuel(truck.getMaxCapacity());
    } else if (nextNode.getNode().getType() == NodeType.DELIVERY) {
      Order order = network.getOrders().stream().filter(o -> o.getId().equals(nextNode.getNode().getId()))
          .findFirst().orElse(null);
      assert order != null;

      int glpToDeliver = Math.min(truck.getCurrentCapacity(), order.getRequestedGLP() - order.getAssignedGLP());

      if (order.getAssignedGLP() + glpToDeliver == order.getRequestedGLP()) {
        order.setAssignedGLP(order.getRequestedGLP());
        nodesLeft.remove(nextNode.getNode());
      } else
        order.setAssignedGLP(order.getAssignedGLP() + glpToDeliver);
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

