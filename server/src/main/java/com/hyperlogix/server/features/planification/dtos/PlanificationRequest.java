package com.hyperlogix.server.features.planification.dtos;

import com.hyperlogix.server.domain.PLGNetwork;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlanificationRequest {
  private String sessionId;
  private PLGNetwork plgNetwork;
}