package com.hyperlogix.server.features.incidents.utils;

import com.hyperlogix.server.domain.Incident;
import com.hyperlogix.server.domain.IncidentType;
import com.hyperlogix.server.features.incidents.entity.IncidentEntity;
import com.hyperlogix.server.features.trucks.TruckRepositoryHolder;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class IncidentMapper {

    public static IncidentEntity mapToEntity(Incident incident) {
        if (incident == null) {
            return null;
        }
        
        IncidentEntity entity = new IncidentEntity();
        if (incident.getId() != null) {
            try {
                entity.setId(Long.parseLong(incident.getId()));
            } catch (NumberFormatException e) {
                entity.setId(null);
            }
        }
        entity.setTurn(incident.getTurn());
        entity.setType(incident.getType());
        // Look up TruckEntity by code and set it
        if (TruckRepositoryHolder.truckRepository != null) {
            var truck = TruckRepositoryHolder.truckRepository.findByCode(incident.getTruckCode());
            entity.setTruck(truck);
        } else {
            entity.setTruck(null); // or throw if strict
        }
        
        return entity;
    }

    public static Incident mapToDomain(IncidentEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Incident incident = new Incident();
        if (entity.getId() != null) {
            incident.setId(entity.getId().toString());
        }
        incident.setTurn(entity.getTurn());
        incident.setType(entity.getType());
        incident.setTruckCode(entity.getTruck().getCode());
        
        return incident;
    }
}
