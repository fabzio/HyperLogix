package com.hyperlogix.server.services.simulation;

import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.services.planification.PlanificationStatus;

import java.time.LocalDateTime;
import java.util.HashMap;

public record SimulationSnapshot(
        LocalDateTime timestamp,
        LocalDateTime simulationTime,
        PLGNetwork plgNetwork,
        Routes routes,
        HashMap<String, Double> truckProgress,
        SimulationMetrics metrics,
        PlanificationStatus planificationStatus) {
}
