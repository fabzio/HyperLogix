package com.hyperlogix.server.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
public class Stop {
  Node node;
  LocalDateTime arrivalTime;
}
