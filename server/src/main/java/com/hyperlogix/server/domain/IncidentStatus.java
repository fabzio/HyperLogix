package com.hyperlogix.server.domain;

/**
 * Estado de un incidente durante su ciclo de vida
 */
public enum IncidentStatus {
    IMMOBILIZED,     // Unidad inmovilizada en el lugar del incidente
    IN_MAINTENANCE,  // Unidad en mantenimiento (para tipos 2 y 3)
    RESOLVED        // Incidente resuelto, unidad disponible
}
