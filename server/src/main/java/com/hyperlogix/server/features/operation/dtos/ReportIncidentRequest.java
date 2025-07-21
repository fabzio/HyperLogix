package com.hyperlogix.server.features.operation.dtos;

import com.hyperlogix.server.domain.IncidentType;
import lombok.Data;

@Data
public class ReportIncidentRequest {
  private String truckCode;
  private IncidentType incidentType;
  private String turn;
}
