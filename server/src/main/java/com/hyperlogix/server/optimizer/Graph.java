package com.hyperlogix.server.optimizer;

import com.hyperlogix.server.config.Constants;
import com.hyperlogix.server.domain.*;
import com.hyperlogix.server.optimizer.AntColony.AntColonyConfig;
import com.hyperlogix.server.util.AStar;
import lombok.Data;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class Graph implements Cloneable {
  private final PLGNetwork plgNetwork;
  private final AntColonyConfig antColonyConfig;
  private final List<Incident> incidents;
  private LocalDateTime algorithmStartDate;
  @Setter
  private Map<Node, Map<Node, Double>> pheromoneMap;

  private Map<Node, Map<Node, Path>> adjacencyMapCache;
  private LocalDateTime lastAdjacencyMapUpdateTime;
  private Set<Roadblock> lastActiveRoadblocks;

  public Graph(PLGNetwork network, LocalDateTime algorithmStartDate, AntColonyConfig antColonyConfig) {
    this.plgNetwork = network;
    this.algorithmStartDate = algorithmStartDate;
    this.antColonyConfig = antColonyConfig;
    this.incidents = List.of();
    this.pheromoneMap = createPheromoneMap();
    this.adjacencyMapCache = null;
    this.lastAdjacencyMapUpdateTime = null;
    this.lastActiveRoadblocks = new HashSet<>(); // Initialize to empty set
  }

  public Graph(PLGNetwork network, LocalDateTime algorithmStartDate, AntColonyConfig antColonyConfig, List<Incident> incidents) {
    this.plgNetwork = network;
    this.algorithmStartDate = algorithmStartDate;
    this.antColonyConfig = antColonyConfig;
    this.incidents = incidents != null ? incidents : List.of();
    this.pheromoneMap = createPheromoneMap();
    this.adjacencyMapCache = null;
    this.lastAdjacencyMapUpdateTime = null;
    this.lastActiveRoadblocks = new HashSet<>(); // Initialize to empty set
  }
  public Map<Node, Map<Node, Path>> createAdjacencyMap(LocalDateTime currentTime) {
    // For adjacency map creation, we use Manhattan distance without A*
    List<Node> ordersNode = plgNetwork.getCalculatedOrders().stream()
        .map(Node::new)
        .toList();
    List<Node> stationsNodes = plgNetwork.getStations().stream()
        .map(Node::new)
        .toList();
    List<Node> incidentNodes = incidents.stream()
        .map(Node::new)
        .toList();

    List<Node> allNodes = new java.util.ArrayList<>(ordersNode);
    allNodes.addAll(stationsNodes);
    allNodes.addAll(incidentNodes);
    Map<Node, Map<Node, Path>> newAdjacencyMap = new HashMap<>();

    for (int i = 0; i < allNodes.size(); i++) {
      Node origin = allNodes.get(i);
      for (int j = i + 1; j < allNodes.size(); j++) {
        Node destination = allNodes.get(j);

        newAdjacencyMap.putIfAbsent(origin, new HashMap<>());
        newAdjacencyMap.putIfAbsent(destination, new HashMap<>());

        // Use Manhattan distance instead of A*
        int manhattanDistance = calculateManhattanDistance(origin.getLocation(), destination.getLocation());
        // Create a simple path with just start and end points for distance calculation
        List<Point> simplePath = List.of(origin.getLocation(), destination.getLocation());
        Path path = new Path(simplePath, manhattanDistance);

        newAdjacencyMap.get(origin).put(destination, path);
        newAdjacencyMap.get(destination).put(origin, path);
      }
    }

    this.adjacencyMapCache = newAdjacencyMap;
    this.lastAdjacencyMapUpdateTime = currentTime;
    this.lastActiveRoadblocks = new HashSet<>(); // Reset since we're not using roadblocks for adjacency
    return newAdjacencyMap;
  }

  private int calculateManhattanDistance(Point from, Point to) {
    return (int) ((Math.abs(from.x() - to.x()) + Math.abs(from.y() - to.y())) * Constants.EDGE_LENGTH);
  }

  /**
   * Process the final routes using A* pathfinding to get exact paths and arrival
   * times
   */
  public Routes processRoutesWithAStar(Routes routes, LocalDateTime algorithmStartTime) {
    Map<String, List<Stop>> processedRoutes = new HashMap<>();
    Map<String, List<Path>> processedPaths = new HashMap<>();
    double totalCost = 0.0;

    for (Map.Entry<String, List<Stop>> entry : routes.getStops().entrySet()) {
      String truckId = entry.getKey();
      List<Stop> originalRoute = entry.getValue();

      if (originalRoute.isEmpty()) {
        processedRoutes.put(truckId, new ArrayList<>());
        processedPaths.put(truckId, new ArrayList<>());
        continue;
      }

      List<Stop> processedRoute = new ArrayList<>();
      List<Path> processedPathList = new ArrayList<>();

      // Add the first stop (truck's starting location)
      Stop currentStop = originalRoute.get(0);
      currentStop.setArrivalTime(algorithmStartTime);
      processedRoute.add(currentStop);

      // Process each subsequent stop with A* pathfinding
      for (int i = 1; i < originalRoute.size(); i++) {
        Stop nextStop = originalRoute.get(i);
        Point fromLocation = currentStop.getNode().getLocation();
        Point toLocation = nextStop.getNode().getLocation();

        // Use A* to find the actual path considering roadblocks
        List<Point> actualPath = AStar.encontrarRuta(
            fromLocation,
            toLocation,
            currentStop.getArrivalTime(),
            plgNetwork.getRoadblocks());

        if (actualPath.isEmpty()) {
          // If A* fails, use direct path as fallback
          actualPath = List.of(fromLocation, toLocation);
        }

        // Calculate actual distance as sum of Manhattan distances between consecutive
        // points
        int totalDistance = 0;
        for (int j = 0; j < actualPath.size() - 1; j++) {
          totalDistance += calculateManhattanDistance(actualPath.get(j), actualPath.get(j + 1));
        }

        Path realPath = new Path(actualPath, totalDistance);
        processedPathList.add(realPath);

        // Calculate actual arrival time based on path length
        Truck truck = plgNetwork.getTrucks().stream()
            .filter(t -> t.getId().equals(truckId))
            .findFirst()
            .orElse(null);

        if (truck != null) {
          Duration travelTime = truck.getTimeToDestination(realPath.length());
          LocalDateTime arrivalTime = currentStop.getArrivalTime().plus(travelTime);
          nextStop.setArrivalTime(arrivalTime);

          double fuelCost = truck.getFuelConsumption(realPath.length());
          totalCost += fuelCost;
        }

        processedRoute.add(nextStop);
        currentStop = nextStop;
      }

      processedRoutes.put(truckId, processedRoute);
      processedPaths.put(truckId, processedPathList);
    }

    return new Routes(processedRoutes, processedPaths, totalCost);
  }

  public Map<Node, Map<Node, Double>> createPheromoneMap() {
    Map<Node, Map<Node, Double>> pheromoneMap = new HashMap<>();
    List<Node> ordersNode = plgNetwork.getOrders().stream()
        .map(Node::new)
        .toList();
    List<Node> stationsNodes = plgNetwork.getStations().stream()
        .map(Node::new)
        .toList();
    List<Node> incidentNodes = incidents.stream()
        .map(Node::new)
        .toList();
    List<Node> allNodes = new java.util.ArrayList<>(ordersNode);
    allNodes.addAll(stationsNodes);
    allNodes.addAll(incidentNodes);
    for (Node origin : allNodes) {
      pheromoneMap.put(origin, new HashMap<>());
      for (Node destination : allNodes) {
        if (!origin.equals(destination)) {
          pheromoneMap.get(origin).put(destination, antColonyConfig.INITIAL_PHEROMONE());
        }
      }
    }
    return pheromoneMap;
  }

  public void updatePheromoneMap(List<Routes> solutions, AntColonyConfig antColonyConfig) {
    // Evaporate pheromones
    for (Node origin : pheromoneMap.keySet()) {
      for (Node destination : pheromoneMap.get(origin).keySet()) {
        pheromoneMap.get(origin).compute(destination, (k, pheromone) -> pheromone * (1 - antColonyConfig.RHO()));
      }
    }
    // Add pheromones based on solutions
    for (Routes solution : solutions) {
      double addPheromone = antColonyConfig.Q() / solution.getCost();
      for (String truck : solution.getStops().keySet()) {
        List<Stop> route = solution.getStops().get(truck);
        for (int i = 1; i < route.size() - 1; i++) {
          Node origin = route.get(i).getNode();
          Node destination = route.get(i + 1).getNode();
          pheromoneMap.get(origin).compute(destination, (k, pheromone) -> pheromone + addPheromone);
        }
      }
    }
  }

  @Override
  public Graph clone() {
    try {
      Graph cloned = (Graph) super.clone();
      cloned.pheromoneMap = new HashMap<>();
      for (Map.Entry<Node, Map<Node, Double>> entry : this.pheromoneMap.entrySet()) {
        cloned.pheromoneMap.put(entry.getKey(), new HashMap<>(entry.getValue()));
      }

      // Clone adjacencyMapCache
      if (this.adjacencyMapCache != null) {
        cloned.adjacencyMapCache = new HashMap<>();
        for (Map.Entry<Node, Map<Node, Path>> entry : this.adjacencyMapCache.entrySet()) {
          // Assuming Path objects are immutable or can be shared.
          // If Path objects are mutable and their state affects caching, a deep clone of
          // Paths might be needed.
          cloned.adjacencyMapCache.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
      } else {
        cloned.adjacencyMapCache = null;
      }

      cloned.lastAdjacencyMapUpdateTime = this.lastAdjacencyMapUpdateTime; // LocalDateTime is immutable

      if (this.lastActiveRoadblocks != null) {
        cloned.lastActiveRoadblocks = new HashSet<>(this.lastActiveRoadblocks); // Roadblock is a record (immutable by
                                                                                // default)
      } else {
        cloned.lastActiveRoadblocks = null;
      }

      return cloned;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }

}
