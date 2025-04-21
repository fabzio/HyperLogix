package com.hyperlogix.server.optimizer.AntColony;

public record AntColonyConfig(
                int NUM_ANTS,
                int NUM_ITERATIONS,
                double ALPHA,
                double BETA,
                double RHO,
                double Q,
                double INITIAL_PHEROMONE) {
}
