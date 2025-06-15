package com.hyperlogix.server.features.incidents.usecases;

import com.hyperlogix.server.domain.Incident;
import com.hyperlogix.server.domain.TruckState;
import com.hyperlogix.server.features.incidents.repository.IncidentRepository;
import com.hyperlogix.server.features.incidents.utils.IncidentMapper;
import com.hyperlogix.server.features.trucks.repository.TruckRepository;
import com.hyperlogix.server.features.trucks.entity.TruckEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalTime;

@Service
public class RegisterIncidentUseCase {
    private final IncidentRepository incidentRepository;
    private final TruckRepository truckRepository;

    public RegisterIncidentUseCase(IncidentRepository incidentRepository, TruckRepository truckRepository) {
        this.incidentRepository = incidentRepository;
        this.truckRepository = truckRepository;
    }

    @Transactional
    public Incident registerIncident(Incident incident) {
        // Validate truck exists
        TruckEntity truck = truckRepository.findByCode(incident.getTruckCode());
        if (truck == null) {
            throw new IllegalArgumentException("No se encontró el camión: " + incident.getTruckCode());
        }

        // Determine turn based on time
        LocalTime time = LocalTime.now();
        String turn;
        if (time.isBefore(LocalTime.of(8, 0))) {
            turn = "T1";
        } else if (time.isBefore(LocalTime.of(16, 0))) {
            turn = "T2";
        } else {
            turn = "T3";
        }
        incident.setTurn(turn);

        // Update truck status
        truck.setStatus(TruckState.BROKEN_DOWN);
        truckRepository.save(truck);

        // Save incident
        return IncidentMapper.mapToDomain(incidentRepository.save(IncidentMapper.mapToEntity(incident)));
    }
}
