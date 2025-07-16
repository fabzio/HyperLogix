package com.hyperlogix.server.features.planification.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hyperlogix.server.features.planification.services.LogisticCollapseDetectionService;

@RestController
@RequestMapping("/api/collapse")
public class LogisticCollapseController {

    @Autowired
    private LogisticCollapseDetectionService collapseDetectionService;

    /**
     * Endpoint para reportar un colapso logístico manualmente
     */
    @PostMapping("/report")
    public ResponseEntity<String> reportCollapse(
            @RequestParam String sessionId,
            @RequestParam String collapseType,
            @RequestParam String description,
            @RequestParam double severityLevel,
            @RequestParam String affectedArea) {

        try {
            collapseDetectionService.reportManualCollapse(
                sessionId, collapseType, description, severityLevel, affectedArea);

            return ResponseEntity.ok("Colapso logístico reportado exitosamente");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error al reportar colapso: " + e.getMessage());
        }
    }

    /**
     * Endpoint para información sobre tipos de colapso disponibles
     */
    @GetMapping("/types")
    public ResponseEntity<String[]> getCollapseTypes() {
        String[] collapseTypes = {
            "NO_ROUTES",
            "ROUTE_SATURATION",
            "ORDER_BACKLOG",
            "DELIVERY_DELAY",
            "RESOURCE_SHORTAGE",
            "TRAFFIC_CONGESTION",
            "SYSTEM_OVERLOAD"
        };

        return ResponseEntity.ok(collapseTypes);
    }
}
