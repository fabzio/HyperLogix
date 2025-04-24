package com.hyperlogix.server.optimizer.AntColony;

/**
 * Clase de configuración para el algoritmo de Optimización por Colonia de Hormigas.
 * @param NUM_ANTS Número de hormigas en la colonia.
 * @param NUM_ITERATIONS Número de iteraciones para el algoritmo.
 * @param ALPHA Influencia de la feromona en la selección del camino.
 * @param BETA Influencia de la información heurística en la selección del camino.
 * @param RHO Tasa de evaporación de la feromona.
 * @param Q Cantidad de feromona depositada en el camino.
 * @param INITIAL_PHEROMONE Nivel inicial de feromona en los caminos.
 */
public record AntColonyConfig(
                int NUM_ANTS,
                int NUM_ITERATIONS,
                double ALPHA,
                double BETA,
                double RHO,
                double Q,
                double INITIAL_PHEROMONE) {
}
