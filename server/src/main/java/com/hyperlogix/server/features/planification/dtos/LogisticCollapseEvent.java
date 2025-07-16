package com.hyperlogix.server.features.planification.dtos;

import java.time.LocalDateTime;

public class LogisticCollapseEvent {
  private String sessionId;
  private String collapseType;
  private String description;
  private LocalDateTime timestamp;
  private double severityLevel;
  private String affectedArea;

  public LogisticCollapseEvent(String sessionId, String collapseType, String description,
                              LocalDateTime timestamp, double severityLevel, String affectedArea) {
    this.sessionId = sessionId;
    this.collapseType = collapseType;
    this.description = description;
    this.timestamp = timestamp;
    this.severityLevel = severityLevel;
    this.affectedArea = affectedArea;
  }

  // Getters
  public String getSessionId() { return sessionId; }
  public String getCollapseType() { return collapseType; }
  public String getDescription() { return description; }
  public LocalDateTime getTimestamp() { return timestamp; }
  public double getSeverityLevel() { return severityLevel; }
  public String getAffectedArea() { return affectedArea; }

  // Setters
  public void setSessionId(String sessionId) { this.sessionId = sessionId; }
  public void setCollapseType(String collapseType) { this.collapseType = collapseType; }
  public void setDescription(String description) { this.description = description; }
  public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
  public void setSeverityLevel(double severityLevel) { this.severityLevel = severityLevel; }
  public void setAffectedArea(String affectedArea) { this.affectedArea = affectedArea; }
}
