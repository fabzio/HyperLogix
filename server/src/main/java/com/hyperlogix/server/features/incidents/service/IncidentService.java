package com.hyperlogix.server.features.incidents.service;

import com.hyperlogix.server.features.incidents.entity.IncidentEntity;
import com.hyperlogix.server.domain.IncidentType;
import com.hyperlogix.server.features.incidents.repository.IncidentRepository;
import com.hyperlogix.server.features.trucks.repository.TruckRepository;
import com.hyperlogix.server.features.trucks.entity.TruckEntity;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final TruckRepository truckRepository;

    public IncidentService(IncidentRepository incidentRepository, TruckRepository truckRepository) {
        this.incidentRepository = incidentRepository;
        this.truckRepository = truckRepository;
    }

    public IncidentEntity createIncident(String truckCode, String type, String turn) {
        // First, find the truck by its code
        TruckEntity truck = truckRepository.findByCode(truckCode);
        if (truck == null) {
            throw new EntityNotFoundException("Truck not found with code: " + truckCode);
        }

        // Create new incident
        IncidentEntity incident = new IncidentEntity();
        incident.setTruck(truck); // Set the TruckEntity, not just the code
        incident.setType(IncidentType.valueOf(type));
        incident.setTurn(turn);

        // Save and return the incident
        return incidentRepository.save(incident);
    }

    public void closeIncident(Long incidentId) {
        IncidentEntity incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new EntityNotFoundException("Incident not found with id: " + incidentId));
        
        incidentRepository.save(incident);
    }
}
