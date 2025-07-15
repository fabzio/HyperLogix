package com.hyperlogix.server.features.planification.dtos;

import java.util.List;

import com.hyperlogix.server.domain.Incident;
import com.hyperlogix.server.domain.Routes;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class PlanificationResultNotification {
    private Routes routes;
    private List<Incident> incidents;
}
