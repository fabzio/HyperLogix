package com.hyperlogix.server.services.planification;

public record PlanificationStatus(
    boolean planning,
    int currentNodesProcessed) {
}
