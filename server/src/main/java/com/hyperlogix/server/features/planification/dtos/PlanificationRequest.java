package com.hyperlogix.server.features.planification.dtos;

import com.hyperlogix.server.domain.PLGNetwork;

public class PlanificationRequest {
  private String sessionId;
  private PLGNetwork plgNetwork;

  public PlanificationRequest() {}

  public PlanificationRequest(String sessionId, PLGNetwork plgNetwork) {
    this.sessionId = sessionId;
    this.plgNetwork = plgNetwork;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public PLGNetwork getPlgNetwork() {
    return plgNetwork;
  }

  public void setPlgNetwork(PLGNetwork plgNetwork) {
    this.plgNetwork = plgNetwork;
  }
}