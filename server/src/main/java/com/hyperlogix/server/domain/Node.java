package com.hyperlogix.server.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Node {
  String id;
  String name;
  NodeType type;
  Point location;

  public Node(Station station) {
    this.id = station.getId();
    this.name = station.getName();
    this.type = NodeType.STATION;
    this.location = station.getLocation();
  }

  public Node(Order order) {
    this.id = order.getId();
    this.name = order.getClientId();
    this.type = NodeType.DELIVERY;
    this.location = order.getLocation();
  }
}
