package com.hyperlogix.server.optimizer.AntColony;

import com.hyperlogix.server.config.Constants;
import com.hyperlogix.server.optimizer.Graph;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import java.time.format.DateTimeFormatter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.ApplicationEventPublisher;

import com.hyperlogix.server.domain.Node;
import com.hyperlogix.server.domain.NodeType;
import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Path;
import com.hyperlogix.server.domain.Point;
import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.domain.Station;
import com.hyperlogix.server.domain.Stop;
import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.domain.TruckState;
import com.hyperlogix.server.domain.Incident;
import com.hyperlogix.server.domain.Roadblock;
import com.hyperlogix.server.util.AStar;
import com.hyperlogix.server.features.planification.dtos.LogisticCollapseEvent;

public class Ant {
  private PLGNetwork network;
  private final PLGNetwork originalNetwork;
  private List<Incident> incidentList;
  private List<Roadblock> roadblocks; // Para detectar zonas bloqueadas
  private Map<String, Point> relocatedOrders; // Para trackear pedidos reubicados
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

  public Ant(PLGNetwork network, Graph graph, AntColonyConfig antColonyConfig, List<Incident> incidents) {
    this.originalNetwork = network.clone();
    this.graph = graph;
    this.antColonyConfig = antColonyConfig;
    this.incidentList = incidents;
    this.roadblocks = new ArrayList<>(); // Inicializar lista vacía
    this.relocatedOrders = new HashMap<>(); // Trackear reubicaciones

    network.getTrucksCapacity();
    resetState();
  }

  public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public void setRoadblocks(List<Roadblock> roadblocks) {
    this.roadblocks = roadblocks != null ? roadblocks : new ArrayList<>();
  }

  public Routes findSolution() {

    for (Truck truck : network.getTrucks()) {
      if (truck.getStatus() == TruckState.MAINTENANCE) {
        handleMaintenanceTruckRoute(truck);
        continue;
      }
      Stop firstNode = new Stop(
          new Node(truck.getId(), truck.getType().toString(), NodeType.LOCATION, truck.getLocation().integerPoint()),
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
              "Flota de vehículos");
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

  private void handleMaintenanceTruckRoute(Truck truck) {
    Stop currentNode = new Stop(
        new Node(truck.getCode(), truck.getType().toString(), NodeType.LOCATION, truck.getLocation().integerPoint()),
        graph.getAlgorithmStartDate());

    routes.put(truck.getId(), new ArrayList<>(List.of(currentNode)));

    // Verificar si el camión ya está en una estación
    if (isAtStation(truck)) {
      // Si ya está en una estación, no necesita moverse
      return;
    }

    // Encontrar la estación más cercana
    Station nearestStation = findNearestStation(truck);
    if (nearestStation == null) {
      System.out.println("Warning: No available station found for maintenance truck " + truck.getCode());
      return;
    }

    // Crear nodo de destino (estación)
    Node stationNode = new Node(nearestStation.getId(), "STATION", NodeType.STATION,
        nearestStation.getLocation().integerPoint());

    // Calcular distancia y tiempo
    int distance = calculateManhattanDistance(truck.getLocation().integerPoint(),
        nearestStation.getLocation().integerPoint());
    Duration timeToDestination = truck.getTimeToDestination(distance);
    LocalDateTime arrivalTime = graph.getAlgorithmStartDate().plus(timeToDestination);

    // Verificar si tiene suficiente combustible
    double fuelConsumption = truck.getFuelConsumption(distance);
    if (truck.getCurrentFuel() < fuelConsumption) {
      System.out.println(
          "Warning: Truck " + truck.getCode() + " in maintenance doesn't have enough fuel to reach nearest station");
      // En un escenario real, podrías necesitar enviar un camión de combustible o
      // grúa
      return;
    }

    Stop stationStop = new Stop(stationNode, arrivalTime);
    Path pathToStation = new Path(
        List.of(currentNode.getNode().getLocation(), stationNode.getLocation()),
        distance);
    firstPath.put(stationNode, pathToStation);

    // Agregar la ruta a la estación
    moveToNode(truck, currentNode, stationStop);

    System.out.println("Maintenance truck " + truck.getCode() + " routed to station " + nearestStation.getId());
  }

  private boolean isAtStation(Truck truck) {
    Point truckLocation = truck.getLocation().integerPoint();
    return network.getStations().stream()
        .anyMatch(station -> station.getLocation().integerPoint().equals(truckLocation));
  }

  // Método auxiliar para encontrar la estación más cercana
  private Station findNearestStation(Truck truck) {
    Point truckLocation = truck.getLocation().integerPoint();

    return network.getStations().stream()
        .filter(station -> {
          // Verificar que la estación tenga capacidad disponible
          int distance = calculateManhattanDistance(truckLocation, station.getLocation().integerPoint());
          Duration timeToDestination = truck.getTimeToDestination(distance);
          LocalDateTime arrivalTime = graph.getAlgorithmStartDate().plus(timeToDestination);
          return station.getAvailableCapacity(arrivalTime) > 0;
        })
        .min((s1, s2) -> {
          int dist1 = calculateManhattanDistance(truckLocation, s1.getLocation().integerPoint());
          int dist2 = calculateManhattanDistance(truckLocation, s2.getLocation().integerPoint());
          return Integer.compare(dist1, dist2);
        })
        .orElse(null);
  }

  private Truck selectBestTruck() {
    Truck bestTruck = null;
    double bestScore = 0;

    for (Truck truck : network.getTrucks()) {
      if (truck.getStatus() == TruckState.MAINTENANCE || truck.getStatus() == TruckState.BROKEN_DOWN) {
        continue;
      }
      Stop currentNode = routes.get(truck.getId()).getLast();
      List<Stop> availableNodes = getAvailableNodes(truck, currentNode);

      if (availableNodes.isEmpty()) {
        continue;
      }

      // Simple scoring: prioritize IDLE trucks, then by fuel level
      double score = truck.getStatus() == TruckState.IDLE ? 2.0 : 0.5;
      score *= truck.getCurrentFuel() / truck.getFuelCapacity();

      if (score > bestScore) {
        bestScore = score;
        bestTruck = truck;
      }
    }

    return bestTruck;
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
            .filter(
                entry -> entry.getKey().getType() == NodeType.STATION || entry.getKey().getType() == NodeType.DELIVERY)
            .map(entry -> entry.getValue().length()).mapToDouble(truck::getFuelConsumption).min()
            .orElse(Double.POSITIVE_INFINITY);
        if (fuelToNearestStation > fuelAfterDelivery)
          continue;
      } else if (node.getType() == NodeType.INCIDENT) {

        Incident incident = incidentList.stream()
            .filter(i -> i.getTruckCode().equals(node.getId()))
            .findFirst().orElse(null);

        assert incident != null;
        if (incident.getFuel() == 0)
          continue;

        if ((truck.getMaxCapacity() - truck.getCurrentCapacity()) == 0)
          continue;

        if (currentNode.getArrivalTime().plus(timeToDestination).isAfter(incident.getExpectedRecovery()))
          continue;

        // Verificar si hay suficiente combustible para ir al incidente y luego a la
        // estación más cercana
        double fuelAfterVisit = truck.getCurrentFuel() - fuelConsumption;

        double fuelToNearestStation = adjacencyMap.get(node).entrySet().stream()
            .filter(
                entry -> entry.getKey().getType() == NodeType.STATION || entry.getKey().getType() == NodeType.DELIVERY)
            .map(entry -> entry.getValue().length()).mapToDouble(truck::getFuelConsumption).min()
            .orElse(Double.POSITIVE_INFINITY);

        if (fuelToNearestStation > fuelAfterVisit)
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
      double pheromone;
      if (node.getNode().getType() == NodeType.INCIDENT) {
        // Use the highest pheromone value among station nodes
        pheromone = adjacencyMap.get(currentNode.getNode()).entrySet().stream()
            .filter(entry -> entry.getKey().getType() == NodeType.STATION)
            .map(entry -> graph.getPheromoneMap().get(currentNode.getNode()).get(entry.getKey()))
            .max(Double::compare)
            .orElse(graph.getPheromoneMap().get(currentNode.getNode()).get(node.getNode()));
      } else {
        pheromone = graph.getPheromoneMap().get(currentNode.getNode()).get(node.getNode());
      }

      double penalization = 1;
      switch (node.getNode().getType()) {
        case NodeType.STATION:
          double capacityFactor = 1 + (double) truck.getCurrentCapacity() / truck.getMaxCapacity();
          penalization = capacityFactor;
          break;

        case NodeType.INCIDENT:
          // Priorizar incidentes basados en su severidad y carga disponible para
          // transferir
          Incident incident = incidentList.stream()
              .filter(i -> i.getId().equals(node.getNode().getId()))
              .findFirst().orElse(null);

          Truck accidentedTruck = network.getTrucks().stream()
              .filter(t -> incident != null && t.getCode().equals(incident.getTruckCode()))
              .findFirst().orElse(null);

          double severityFactor = 1.0;
          if (incident != null && accidentedTruck != null) {
            int incidentDistance = calculateManhattanDistance(accidentedTruck.getLocation().integerPoint(),
                node.getNode().getLocation());
            int maxDistance = adjacencyMap.get(currentNode.getNode()).entrySet().stream()
                .mapToInt(entry -> {
                  return calculateManhattanDistance(accidentedTruck.getLocation().integerPoint(),
                      entry.getKey().getLocation());
                })
                .max().orElse(1);
            severityFactor = 1.0 + ((double) incidentDistance / Math.max(maxDistance, 1));
          }

          double transferFactor = 1.0;
          if (accidentedTruck != null && accidentedTruck.getCurrentCapacity() > 0) {
            // Si hay mucha carga para transferir, es más prioritario
            transferFactor = 0.5
                + (0.5 * (1.0 - (double) accidentedTruck.getCurrentCapacity() / accidentedTruck.getMaxCapacity()));
          }
          penalization = severityFactor * transferFactor;
          break;

        case NodeType.DELIVERY:
          // Código existente para DELIVERY...
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

          double urgencyFactor = 1 - Math.min((double) minutesLeft / maxTimeLeft, 1.0); // Más cerca del deadline =
                                                                                        // mayor
                                                                                        // urgencia
          double urgencyPenaltyScale = 0.5;
          double urgencyPenalty = 1 + urgencyPenaltyScale * urgencyFactor;

          penalization = urgencyPenalty / (1 + (double) truck.getCurrentCapacity() / truck.getMaxCapacity());
          break;

        case NodeType.LOCATION:
          break;
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
    
    // CRITICAL FIX: Validate path is not null to prevent NullPointerException
    if (path == null) {
      System.out.println("WARNING: NULL PATH detected from " + currentNode.getNode().getId() + 
                        " to " + nextNode.getNode().getId() + ". Creating emergency path.");
      
      // Create emergency direct path with Manhattan distance
      var from = currentNode.getNode().getLocation();
      var to = nextNode.getNode().getLocation();
      int emergencyDistance = (int)(Math.abs(from.x() - to.x()) + Math.abs(from.y() - to.y()) * Constants.EDGE_LENGTH);
      
      // Create simple emergency path with just start and end points
      path = new Path(List.of(from, to), emergencyDistance);
    }
    
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
    }

    else if (nextNode.getNode().getType() == NodeType.INCIDENT) {
      // Obtener el camión accidentado y el incidente
      Incident incident = incidentList.stream()
          .filter(i -> i.getTruckCode().equals(nextNode.getNode().getId()))
          .findFirst().orElse(null);

      if (incident != null) {
        // Encontrar el camión asociado al incidente
        Truck accidentedTruck = network.getTrucks().stream()
            .filter(t -> t.getCode().equals(incident.getTruckCode()))
            .findFirst().orElse(null);

        if (accidentedTruck != null && accidentedTruck.getCurrentCapacity() > 0) {
          // Transferir una parte de la carga del camión accidentado
          int transferAmount = Math.min(
              accidentedTruck.getCurrentCapacity(),
              truck.getMaxCapacity() - truck.getCurrentCapacity());

          // Actualizar capacidades
          accidentedTruck.setCurrentCapacity(accidentedTruck.getCurrentCapacity() - transferAmount);
          truck.setCurrentCapacity(truck.getCurrentCapacity() + transferAmount);
          double fuelTransfer = Math.min(incident.getFuel(), truck.getFuelCapacity() - truck.getCurrentFuel());
          truck.setCurrentFuel(truck.getCurrentFuel() - fuelConsumption + fuelTransfer);
          incident.setFuel((int) (incident.getFuel() - fuelTransfer));

          if (incident.getFuel() == 0)
            nodesLeft.remove(nextNode.getNode());

        }

      }
    }

    truck.setLocation(nextNode.getNode().getLocation().integerPoint());
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

  // //metodo para debuggear
  // private void writeDebugLog(String message) {
  // try (FileWriter writer = new FileWriter(DEBUG_FILE)) {
  // String timestamp = LocalDateTime.now().format(DEBUG_TIME_FORMAT);
  // writer.write("[" + timestamp + "] ANT- " + message + "\n");
  // } catch (IOException e) {
  // System.err.println("Error writing debug log: " + e.getMessage());
  // }
  // }
}
