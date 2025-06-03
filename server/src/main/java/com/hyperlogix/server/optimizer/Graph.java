package com.hyperlogix.server.optimizer;

import com.hyperlogix.server.config.Constants;
import com.hyperlogix.server.domain.*;
import com.hyperlogix.server.optimizer.AntColony.AntColonyConfig;
import com.hyperlogix.server.util.AStar;
import lombok.Data;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class Graph implements Cloneable {
  private final PLGNetwork plgNetwork;
  private final AntColonyConfig antColonyConfig;
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
    this.pheromoneMap = createPheromoneMap();
    this.adjacencyMapCache = null;
    this.lastAdjacencyMapUpdateTime = null;
    this.lastActiveRoadblocks = new HashSet<>(); // Initialize to empty set
  }

  private Set<Roadblock> getActiveRoadblocks(LocalDateTime currentTime) {
    if (plgNetwork.getRoadblocks() == null) {
      return new HashSet<>();
    }
    return plgNetwork.getRoadblocks().stream()
        .filter(rb -> !currentTime.isBefore(rb.start()) && !currentTime.isAfter(rb.end()))
        .collect(Collectors.toSet());
  }

  public Map<Node, Map<Node, Path>> createAdjacencyMap(LocalDateTime currentTime) {
    Set<Roadblock> currentActiveRoadblocks = getActiveRoadblocks(currentTime);

    if (this.adjacencyMapCache != null &&
        this.lastActiveRoadblocks != null &&
        this.lastActiveRoadblocks.equals(currentActiveRoadblocks)) {
      return this.adjacencyMapCache;
    }

    List<Node> ordersNode = plgNetwork.getCalculatedOrders().stream()
        .map(Node::new)
        .toList();
    List<Node> stationsNodes = plgNetwork.getStations().stream()
        .map(Node::new)
        .toList();

    List<Node> allNodes = new java.util.ArrayList<>(ordersNode);
    allNodes.addAll(stationsNodes);
    Map<Node, Map<Node, Path>> newAdjacencyMap = new HashMap<>();
    for (int i = 0; i < allNodes.size(); i++) {
      Node origin = allNodes.get(i);
      for (int j = i + 1; j < allNodes.size(); j++) {
        Node destination = allNodes.get(j);

        newAdjacencyMap.putIfAbsent(origin, new HashMap<>());
        newAdjacencyMap.putIfAbsent(destination, new HashMap<>());
        List<Point> bestPath = AStar
            .encontrarRuta(origin.getLocation(), destination.getLocation(), currentTime,
                this.plgNetwork.getRoadblocks().stream().filter(rb -> currentActiveRoadblocks.contains(rb))
                    .collect(Collectors.toList())); // Pass only active ones
        Path path = new Path(bestPath, bestPath.size() * Constants.EDGE_LENGTH);

        newAdjacencyMap.get(origin).put(destination, path);
        newAdjacencyMap.get(destination).put(origin, path);
      }
    }
    this.adjacencyMapCache = newAdjacencyMap;
    this.lastAdjacencyMapUpdateTime = currentTime;
    this.lastActiveRoadblocks = currentActiveRoadblocks;
    return newAdjacencyMap;
  }

  public Map<Node, Map<Node, Double>> createPheromoneMap() {
    Map<Node, Map<Node, Double>> pheromoneMap = new HashMap<>();
    List<Node> ordersNode = plgNetwork.getOrders().stream()
        .map(Node::new)
        .toList();
    List<Node> stationsNodes = plgNetwork.getStations().stream()
        .map(Node::new)
        .toList();
    List<Node> allNodes = new java.util.ArrayList<>(ordersNode);
    allNodes.addAll(stationsNodes);
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
        for (int i = 0; i < route.size() - 1; i++) {
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
