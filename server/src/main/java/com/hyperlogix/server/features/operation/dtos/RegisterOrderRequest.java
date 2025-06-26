package com.hyperlogix.server.features.operation.dtos;

import com.hyperlogix.server.domain.Point;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
public class RegisterOrderRequest {
  private String id;
  private String clientId;
  private LocalDateTime date;
  private Point location;
  private int requestedGLP;
  private Duration deliveryLimit;
}
