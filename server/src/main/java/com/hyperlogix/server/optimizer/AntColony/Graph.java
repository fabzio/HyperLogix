package com.hyperlogix.server.optimizer.AntColony;

import com.hyperlogix.server.domain.*;
import com.hyperlogix.server.util.AStar;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Graph {
  private final PLGNetwork plgNetwork;
  private final AntColonyConfig antColonyConfig;
  private LocalDateTime algorithmStartDate;
  private Map<Node, Map<Node, Path>> adjacencyMap;
  private final Map<Node, Map<Node, Double>> pheromoneMap;

  public Graph(PLGNetwork network, LocalDateTime algorithmStartDate, AntColonyConfig antColonyConfig) {
    this.plgNetwork = network;
    this.algorithmStartDate = algorithmStartDate;
    this.antColonyConfig = antColonyConfig;
    updateAdjacencyMap(algorithmStartDate);
    this.pheromoneMap = createPheromoneMap();
  }

  public void updateAdjacencyMap(LocalDateTime currentTime) {
    List<Node> ordersNode = plgNetwork.getOrders().stream()
        .map(Node::new)
        .toList();
    List<Node> stationsNodes = plgNetwork.getStations().stream()
        .map(Node::new)
        .toList();

    List<Node> allNodes = new java.util.ArrayList<>(ordersNode);
    allNodes.addAll(stationsNodes);
    Map<Node, Map<Node, Path>> adjacencyMap = new HashMap<>();
    for (int i = 0; i < allNodes.size(); i++) {
      Node origin = allNodes.get(i);
      for (int j = i + 1; j < allNodes.size(); j++) {
        Node destination = allNodes.get(j);

        adjacencyMap.putIfAbsent(origin, new HashMap<>());
        adjacencyMap.putIfAbsent(destination, new HashMap<>());
        List<Point> bestPath = AStar
            .encontrarRuta(origin.getLocation(), destination.getLocation(), currentTime, plgNetwork.getRoadblocks());
        Path path = new Path(bestPath, bestPath.size());

        adjacencyMap.get(origin).put(destination, path);
        adjacencyMap.get(destination).put(origin, path);
      }
    }
    this.adjacencyMap = adjacencyMap;
  }

  private Map<Node, Map<Node, Double>> createPheromoneMap() {
    Map<Node, Map<Node, Double>> pheromoneMap = new HashMap<>();
    for (Node origin : adjacencyMap.keySet()) {
      pheromoneMap.putIfAbsent(origin, new HashMap<>());
      for (Node destination : adjacencyMap.get(origin).keySet()) {
        pheromoneMap.get(origin).put(destination, antColonyConfig.INITIAL_PHEROMONE());
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
      for (String truck : solution.getRoutes().keySet()) {
        List<Stop> route = solution.getRoutes().get(truck);
        for (int i = 0; i < route.size() - 1; i++) {
          Node origin = route.get(i).getNode();
          Node destination = route.get(i + 1).getNode();
          pheromoneMap.get(origin).compute(destination, (k, pheromone) -> pheromone + addPheromone);
        }
      }
    }
  }
}
