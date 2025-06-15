package com.hyperlogix.server.features.incidents.repository;

import com.hyperlogix.server.features.incidents.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<IncidentEntity, Long> {
    // Add custom query methods here if needed
}
