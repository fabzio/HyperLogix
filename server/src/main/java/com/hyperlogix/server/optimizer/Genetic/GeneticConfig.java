package com.hyperlogix.server.optimizer.Genetic;

public record GeneticConfig(
    int POPULATION_SIZE,
    int NUM_GENERATIONS,
    int TOURNAMENT_SIZE,
    double MUTATION_RATE) {
}
