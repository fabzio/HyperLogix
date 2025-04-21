package com.hyperlogix.server.simulation;

import com.hyperlogix.server.domain.PLGNetwork;

import java.time.LocalDateTime;

public record SimulationSnapshot(
                LocalDateTime timestamp,
                PLGNetwork plgNetwork) {
}
