package com.hyperlogix.server.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents an incident that can affect a truck's operation.
 * TI1: Minor incident - 2h immobilization
 * TI2: Medium incident - 2h immobilization + maintenance until next turn
 * TI3: Major incident - 4h immobilization + 3 days maintenance
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Incident {
    /**
     * Unique identifier for the incident
     */
    private String id;

    /**
     * The turn during which the incident occurred (T1, T2, T3)
     */
    private String turn;

    /**
     * The type of incident (TI1, TI2, TI3)
     */
    private String type;

    /**
     * The code of the truck involved in the incident
     */
    private String truckCode;

    public int daysSinceIncident = 0;
}
