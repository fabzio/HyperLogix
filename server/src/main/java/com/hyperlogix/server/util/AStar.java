package com.hyperlogix.server.util;

import com.hyperlogix.server.config.Constants;
import com.hyperlogix.server.domain.Edge;
import com.hyperlogix.server.domain.Point;
import com.hyperlogix.server.domain.Roadblock;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class AStar {
  public static List<Point> encontrarRuta(Point inicio, Point fin, LocalDateTime tiempoInicio,
      List<Roadblock> bloqueosTemporales) {
    PriorityQueue<Nodo> open = new PriorityQueue<>();
    Map<String, Nodo> allNodes = new HashMap<>();

    Nodo start = new Nodo(inicio, tiempoInicio, null, 0, heuristic(inicio, fin));
    open.add(start);
    allNodes.put(clave(start), start);

    while (!open.isEmpty()) {
      Nodo actual = open.poll();
      if (actual.punto.equals(fin)) {
        return reconstruirRuta(actual);
      }

      for (Point vecino : getVecinos(actual.punto)) {
        double hours = Constants.EDGE_LENGTH / Constants.TRUCK_SPEED;
        LocalDateTime nuevoTiempo = actual.tiempo.plus(Duration.ofHours((long) hours));
        if (esBloqueado(actual.punto, vecino, nuevoTiempo, bloqueosTemporales)) {
          continue;
        }

        double nuevoCosto = actual.costo + 1;
        Nodo vecinoNodo = allNodes.getOrDefault(clave(vecino, nuevoTiempo), new Nodo(vecino, nuevoTiempo));
        if (nuevoCosto < vecinoNodo.costo) {
          vecinoNodo.parent = actual;
          vecinoNodo.costo = nuevoCosto;
          vecinoNodo.heuristic = nuevoCosto + heuristic(vecino, fin);
          if (!open.contains(vecinoNodo)) {
            open.add(vecinoNodo);
          }
          allNodes.put(clave(vecinoNodo), vecinoNodo);
        }
      }
    }

    return Collections.emptyList();
  }
  private static boolean esBloqueado(Point a, Point b, LocalDateTime tiempo, List<Roadblock> bloqueos) {
    for (Roadblock rb : bloqueos) {
      if (!tiempo.isBefore(rb.start()) && !tiempo.isAfter(rb.end())) {
        Edge movimiento = new Edge(a, b);
        for (Edge bloqueado : rb.parseRoadlock()) {
          if (intersect(movimiento, bloqueado)) {
            return true;
          }
        }
      }
    }
    return false;
  }
  
  //Chequear si el movimiento crua entre tras un bloqueo
  private static boolean intersect(Edge e1, Edge e2) {
    Point p1 = e1.from();
    Point p2 = e1.to();
    Point p3 = e2.from();
    Point p4 = e2.to();
    
    if (p1.equals(p3) || p1.equals(p4) || p2.equals(p3) || p2.equals(p4)) {
      return true;
    }
    
    boolean e1Vertical = Math.abs(p1.x() - p2.x()) < 0.0001;
    boolean e2Vertical = Math.abs(p3.x() - p4.x()) < 0.0001;
    
    if (e1Vertical == e2Vertical) {
      return false;
    }
    
    // At this point, one is vertical and one is horizontal
    Edge vertical = e1Vertical ? e1 : e2;
    Edge horizontal = e1Vertical ? e2 : e1;
    
    // Check if the vertical line's x is between the horizontal line's x range
    // and if the horizontal line's y is between the vertical line's y range
    double vx = vertical.from().x();
    double minx = Math.min(horizontal.from().x(), horizontal.to().x());
    double maxx = Math.max(horizontal.from().x(), horizontal.to().x());
    
    double hy = horizontal.from().y();
    double miny = Math.min(vertical.from().y(), vertical.to().y());
    double maxy = Math.max(vertical.from().y(), vertical.to().y());
    
    return vx >= minx && vx <= maxx && hy >= miny && hy <= maxy;
  }

  private static List<Point> getVecinos(Point p) {
    List<Point> vecinos = new ArrayList<>();
    if (p.x() + 1 >= 0)
      vecinos.add(new Point(p.x() + 1, p.y()));
    if (p.x() - 1 >= 0)
      vecinos.add(new Point(p.x() - 1, p.y()));
    if (p.y() + 1 >= 0)
      vecinos.add(new Point(p.x(), p.y() + 1));
    if (p.y() - 1 >= 0)
      vecinos.add(new Point(p.x(), p.y() - 1));
    return vecinos;
  }

  private static double heuristic(Point a, Point b) {
    return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
  }

  private static List<Point> reconstruirRuta(Nodo nodo) {
    List<Point> ruta = new ArrayList<>();
    while (nodo != null) {
      ruta.add(0, nodo.punto);
      nodo = nodo.parent;
    }
    return ruta;
  }

  private static String clave(Nodo n) {
    return clave(n.punto, n.tiempo);
  }

  private static String clave(Point p, LocalDateTime t) {
    return p.x() + "," + p.y() + "," + t.toString();
  }

  private static class Nodo implements Comparable<Nodo> {
    Point punto;
    LocalDateTime tiempo;
    Nodo parent;
    double costo; // g-cost
    double heuristic; // f-cost (g-cost + h-cost)

    Nodo(Point punto, LocalDateTime tiempo, Nodo parent, double costo, double heuristic) {
      this.punto = punto;
      this.tiempo = tiempo;
      this.parent = parent;
      this.costo = costo;
      this.heuristic = heuristic;
    }

    Nodo(Point punto, LocalDateTime tiempo) {
      this(punto, tiempo, null, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    @Override
    public int compareTo(Nodo o) {
      // Compare f-costs (this.heuristic field stores f-cost)
      int fCompare = Double.compare(this.heuristic, o.heuristic);
      if (fCompare == 0) {
        // f-costs are equal, tie-break by h-cost.
        // h-cost = f-cost - g-cost
        double hThis = this.heuristic - this.costo;
        double hOther = o.heuristic - o.costo;
        // Prefer node with smaller h-cost
        return Double.compare(hThis, hOther);
      }
      return fCompare;
    }
  }
}