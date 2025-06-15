package com.hyperlogix.server.features.incidents.controllers;

import com.hyperlogix.server.domain.Incident;
import com.hyperlogix.server.features.incidents.usecases.RegisterIncidentUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/incidents")
public class IncidentController {
    private final RegisterIncidentUseCase registerIncidentUseCase;

    public IncidentController(RegisterIncidentUseCase registerIncidentUseCase) {
        this.registerIncidentUseCase = registerIncidentUseCase;
    }

    @PostMapping
    public ResponseEntity<Incident> registerIncident(@RequestBody Incident incident) {
        return ResponseEntity.ok(registerIncidentUseCase.registerIncident(incident));
    }
}
