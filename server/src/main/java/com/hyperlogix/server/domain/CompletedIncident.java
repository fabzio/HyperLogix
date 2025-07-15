package com.hyperlogix.server.domain;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompletedIncident {
    private String id;
    private String truckId;
    private String type;
    private String turn;
    private Point location;
    private LocalDateTime startTime;
    private LocalDateTime completionTime;
    
    public CompletedIncident(ActiveIncident activeIncident, LocalDateTime completionTime) {
        this.id = activeIncident.getId();
        this.truckId = activeIncident.getTruckId();
        this.type = activeIncident.getType();
        this.turn = activeIncident.getTurn();
        this.location = activeIncident.getLocation();
        this.startTime = activeIncident.getStartTime();
        this.completionTime = completionTime;
    }
    
    public String getTruckCode() {
        return truckId;
    }
}
