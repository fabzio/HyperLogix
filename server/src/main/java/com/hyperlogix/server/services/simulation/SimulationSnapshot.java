package com.hyperlogix.server.services.simulation;

import com.hyperlogix.server.domain.PLGNetwork;

import java.time.LocalDateTime;

public record SimulationSnapshot(
    LocalDateTime timestamp,
    LocalDateTime simulationTime,
    PLGNetwork plgNetwork) {
}
