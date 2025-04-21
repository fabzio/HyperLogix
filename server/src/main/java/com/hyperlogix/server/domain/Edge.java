package com.hyperlogix.server.domain;

public record Edge(
    Point from,
    Point to) {
  public Edge normalize() {
    if (from.x() < to.x() || (from.x() == to.x() && from.y() <= to.y())) {
      return this;
    } else {
      return new Edge(to, from);
    }
  }
}
