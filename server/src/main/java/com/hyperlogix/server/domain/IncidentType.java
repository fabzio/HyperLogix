package com.hyperlogix.server.domain;

public enum IncidentType {
    TI1,  // Flat tire: 2h immobilization
    TI2,  // Engine failure: 2h immobilization + 1 shift maintenance
    TI3   // Crash: 4h immobilization + 3 days maintenance
}
