package com.hyperlogix.server.services.incident;

import com.hyperlogix.server.features.incidents.entity.IncidentEntity;

@FunctionalInterface
public interface IncidentNotifier {
    void notify(IncidentEntity incidentEntity);
}
