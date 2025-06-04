package com.hyperlogix.server.services.simulation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

public record SimulationMetrics(
    // General fleet metrics
    FleetMetrics fleetMetrics,
    
    // Fuel and efficiency metrics
    FuelMetrics fuelMetrics,
    
    // Order and delivery metrics
    DeliveryMetrics deliveryMetrics,
    
    // Route optimization metrics
    RouteMetrics routeMetrics,
    
    // Performance by truck type
    Map<String, TruckTypeMetrics> truckTypeMetrics,
    
    // Performance by customer
    Map<String, CustomerMetrics> customerMetrics,
    
    // Time-based metrics
    TimeMetrics timeMetrics
) {
    
    public static record FleetMetrics(
        int totalTrucks,
        int activeTrucks,
        int trucksInMaintenance,
        int brokenDownTrucks,
        double fleetUtilizationPercentage,
        double averageCapacityUtilization,
        int totalStopsCompleted,
        int totalStopsRemaining
    ) {}
    
    public static record FuelMetrics(
        double totalFuelConsumed,
        double averageFuelConsumptionPerKm,
        double fuelEfficiencyScore,
        int refuelStops,
        double averageFuelLevelAcrossFleet,
        double totalDistanceTraveled,
        Map<String, Double> fuelConsumptionByTruckType
    ) {}
    
    public static record DeliveryMetrics(
        int totalOrders,
        int completedOrders,
        int inProgressOrders,
        int pendingOrders,
        double completionPercentage,
        double averageDeliveryTime,
        double onTimeDeliveryPercentage,
        int totalGLPDelivered,
        int totalGLPRequested,
        double deliveryEfficiencyPercentage,
        double averageOrderValue,
        Map<String, Integer> deliveriesByPriority
    ) {}
    
    public static record RouteMetrics(
        int totalRoutes,
        int activeRoutes,
        double averageRouteLength,
        double averageStopsPerRoute,
        Duration averageRouteCompletionTime,
        double routeOptimizationScore,
        int totalPlanificationRequests,
        Duration averagePlanificationTime,
        double routeDeviationPercentage
    ) {}
    
    public static record TruckTypeMetrics(
        String truckType,
        int truckCount,
        double averageCapacityUtilization,
        double totalFuelConsumed,
        double averageFuelEfficiency,
        int deliveriesCompleted,
        double averageDeliveryTime,
        double utilizationPercentage,
        double averageDistancePerDelivery,
        int maintenanceEvents
    ) {}
    
    public static record CustomerMetrics(
        String customerId,
        String customerName,
        int totalOrders,
        int completedOrders,
        double completionRate,
        double averageDeliveryTime,
        double onTimeDeliveryRate,
        int totalGLPOrdered,
        int totalGLPDelivered,
        double averageOrderSize,
        String priorityLevel,
        Duration averageResponseTime
    ) {}
    
    public static record TimeMetrics(
        LocalDateTime simulationStart,
        LocalDateTime currentSimulationTime,
        Duration totalSimulationTime,
        Duration realTimeElapsed,
        double timeAccelerationFactor,
        LocalDateTime nextPlanificationTime,
        Duration averageStopDuration,
        Duration peakHoursUtilization,
        Map<Integer, Double> hourlyUtilization
    ) {}
}
