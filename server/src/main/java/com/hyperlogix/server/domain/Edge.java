package com.hyperlogix.server.domain;

public record Edge(
    Point from,
    Point to) {

  /** Asegura siempre un orden canónico de los puntos */
  public Edge normalize() {
    if (from.x() < to.x() || (from.x() == to.x() && from.y() <= to.y())) {
      return this;
    } else {
      return new Edge(to, from);
    }
  }

  /** Comprueba si esta arista intersecta (o toca) a otra */
  public boolean intersect(Edge other) {
    Point p1 = this.from;
    Point p2 = this.to;
    Point p3 = other.from;
    Point p4 = other.to;

    // Si comparten extremo
    if (p1.isClose(p3) || p1.isClose(p4) || p2.isClose(p3) || p2.isClose(p4)) {
      return true;
    }

    boolean v1 = Math.abs(p1.x() - p2.x()) < 1e-6; // vertical?
    boolean v2 = Math.abs(p3.x() - p4.x()) < 1e-6;

    // Si ambas son horizontales o ambas verticales, no se cruzan
    if (v1 == v2) {
      return false;
    }

    // Identifica cuál es vertical y cuál horizontal
    Edge vert = v1 ? this : other;
    Edge hor = v1 ? other : this;

    double vx = vert.from.x();
    double minx = Math.min(hor.from.x(), hor.to.x());
    double maxx = Math.max(hor.from.x(), hor.to.x());

    double hy = hor.from.y();
    double miny = Math.min(vert.from.y(), vert.to.y());
    double maxy = Math.max(vert.from.y(), vert.to.y());

    // Comprueba si se cruzan en el rango
    return vx >= minx && vx <= maxx && hy >= miny && hy <= maxy;
  }
}