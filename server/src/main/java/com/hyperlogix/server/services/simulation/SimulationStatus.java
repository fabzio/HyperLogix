package com.hyperlogix.server.services.simulation;


public record SimulationStatus(
    boolean running,
    boolean paused,
    int timeAcceleration) {
}
