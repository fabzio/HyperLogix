package com.hyperlogix.server.optimizer.Genetic;

import com.hyperlogix.server.domain.*;
import com.hyperlogix.server.util.AStar;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class Chromosome {
  private Routes routes;
  private double fitness;

  public Chromosome(Routes routes) {
    this.routes = routes;
    this.fitness = (routes != null) ? routes.getCost() : Double.POSITIVE_INFINITY;
  }

    public void setRoutes(Routes routes) {
    this.routes = routes;
    this.fitness = (routes != null) ? routes.getCost() : Double.POSITIVE_INFINITY;
  }

  public void recalculateFitness(PLGNetwork network, LocalDateTime algorithmStartDate) {
    if (this.routes == null || this.routes.getRoutes() == null) {
      this.fitness = Double.POSITIVE_INFINITY;
      if (this.routes != null) {
        this.routes.setCost(Double.POSITIVE_INFINITY);
      }
      return;
    }

    double totalCost = 0;
    Map<String, List<Stop>> currentRoutes = this.routes.getRoutes();
    Map<String, List<Path>> currentPaths = this.routes.getPaths();
    currentPaths.clear();

    for (Map.Entry<String, List<Stop>> routeEntry : currentRoutes.entrySet()) {
      String truckId = routeEntry.getKey();
      List<Stop> stops = routeEntry.getValue();
      List<Path> newPaths = new ArrayList<>();
      currentPaths.put(truckId, newPaths);

      Truck truck = network.getTrucks().stream()
          .filter(t -> t.getId().equals(truckId))
          .findFirst()
          .orElse(null);
      if (truck == null || stops.isEmpty()) {
        continue;
      }

      double truckRouteCost = 0;
      LocalDateTime currentTime = algorithmStartDate;

        stops.getFirst().setArrivalTime(currentTime);

        for (int i = 0; i < stops.size() - 1; i++) {
        Stop originStop = stops.get(i);
        Stop destinationStop = stops.get(i + 1);

        if (i > 0) {
          originStop.setArrivalTime(
              stops.get(i - 1).getArrivalTime().plus(truck.getTimeToDestination(newPaths.get(i - 1).length())));
        } else {
          originStop.setArrivalTime(algorithmStartDate);
        }
        currentTime = originStop.getArrivalTime();

        List<Point> pathPoints = AStar.encontrarRuta(
            originStop.getNode().getLocation(),
            destinationStop.getNode().getLocation(),
            currentTime,
            network.getRoadblocks());

        if (pathPoints == null || pathPoints.isEmpty()) {
          totalCost = Double.POSITIVE_INFINITY;
          break;
        }

        Path path = new Path(pathPoints, pathPoints.size());
        newPaths.add(path);

        int distance = path.length();
        Duration timeToDestination = truck.getTimeToDestination(distance);
        double fuelConsumption = truck.getFuelConsumption(distance);

        destinationStop.setArrivalTime(currentTime.plus(timeToDestination));

        truckRouteCost += fuelConsumption;

      }

      if (totalCost == Double.POSITIVE_INFINITY) {
        break;
      }
      totalCost += truckRouteCost;
    }

    this.routes.setCost(totalCost);
    this.fitness = totalCost;
  }
}
