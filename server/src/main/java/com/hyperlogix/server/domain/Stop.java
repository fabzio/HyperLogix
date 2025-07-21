package com.hyperlogix.server.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Stop {
  Node node;
  LocalDateTime arrivalTime;
  boolean arrived;

  public Stop(Node node, LocalDateTime arrivalTime) {
    this.node = node;
    this.arrivalTime = arrivalTime;
    this.arrived = false;
  }
  

}
