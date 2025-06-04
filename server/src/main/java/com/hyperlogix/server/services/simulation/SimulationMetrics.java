package com.hyperlogix.server.services.simulation;

public record SimulationMetrics(
    // Fleet utilization
    double fleetUtilizationPercentage,

    // Fuel efficiency
    double averageFuelConsumptionPerKm,

    // Delivery performance
    double completionPercentage,
    double averageDeliveryTimeMinutes,

    // Capacity utilization
    double averageCapacityUtilization,

    // Planning efficiency
    double averagePlanificationTimeSeconds,

    // Distance efficiency
    double totalDistanceTraveled,

    // GLP delivery efficiency
    double deliveryEfficiencyPercentage) {
}
