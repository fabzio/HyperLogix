package com.hyperlogix.server.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Incident {
  private String type;
  private LocalDateTime date = LocalDateTime.now();
  private String truckCode;
}
