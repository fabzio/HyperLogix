package com.hyperlogix.server.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record Roadblock(
    LocalDateTime start,
    LocalDateTime end,
    List<Point> blockedNodes) {
  public Set<Edge> parseRoadlock() {
    Set<Edge> blockedEdges = new HashSet<>();

    List<Point> nodes = this.blockedNodes();
    for (int i = 0; i < nodes.size() - 1; i++) {
      Point origin = nodes.get(i);
      Point destination = nodes.get(i + 1);
      Edge edge = new Edge(origin, destination).normalize();
      blockedEdges.add(edge);
    }

    return blockedEdges;
  }
}
