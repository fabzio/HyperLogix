package com.hyperlogix.server.features.planification.dtos;

import com.hyperlogix.server.domain.Routes;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlanificationResponse {
  private String sessionId;
  private Routes routes;
}