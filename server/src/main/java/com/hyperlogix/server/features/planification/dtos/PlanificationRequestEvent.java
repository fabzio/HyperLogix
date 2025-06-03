package com.hyperlogix.server.features.planification.dtos;

import java.time.LocalDateTime;

import com.hyperlogix.server.domain.PLGNetwork;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlanificationRequestEvent {
  private String sessionId;
  private PLGNetwork plgNetwork;
  private LocalDateTime simulatedTime;
}