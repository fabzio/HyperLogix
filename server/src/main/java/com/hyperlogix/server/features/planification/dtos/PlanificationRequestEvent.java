package com.hyperlogix.server.features.planification.dtos;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Incident;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlanificationRequestEvent {
  private String sessionId;
  private PLGNetwork plgNetwork;
  private LocalDateTime simulatedTime;
  private Duration algorithmDuration;
  private List<Incident> incidents;
}