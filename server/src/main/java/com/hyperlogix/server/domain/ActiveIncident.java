package com.hyperlogix.server.domain;

import java.time.LocalDateTime;
import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActiveIncident {
    private String id;
    private String truckId;
    private String turn;
    private String type;
    private Point location;
    private LocalDateTime startTime;
    private LocalDateTime workShopStartTime;
    
    public ActiveIncident(String id, String truckId, String turn, String type, Point location, LocalDateTime startTime) {
        this.id = id;
        this.truckId = truckId;
        this.turn = turn;
        this.type = type;
        this.location = location;
        this.startTime = startTime;
    }
    
    public boolean isImmobilized(LocalDateTime currentTime) {
        Duration elapsed = Duration.between(startTime, currentTime);
        Duration immobilizationPeriod = getImmobilizationDuration();
        return elapsed.compareTo(immobilizationPeriod) < 0;
    }
    
    public boolean isInWorkshop(LocalDateTime currentTime) {
        if (workShopStartTime == null) return false;
        Duration elapsed = Duration.between(workShopStartTime, currentTime);
        Duration workshopPeriod = getWorkshopDuration();
        return elapsed.compareTo(workshopPeriod) < 0;
    }
    
    public boolean isImmobilizationEnded(LocalDateTime currentTime) {
        Duration elapsed = Duration.between(startTime, currentTime);
        return elapsed.compareTo(getImmobilizationDuration()) >= 0;
    }
    
    public boolean isWorkshopEnded(LocalDateTime currentTime) {
        if (workShopStartTime == null) return false;
        Duration elapsed = Duration.between(workShopStartTime, currentTime);
        return elapsed.compareTo(getWorkshopDuration()) >= 0;
    }
    
    public Duration getImmobilizationDuration() {
        return switch (type) {
            case "TYPE_1", "TI1" -> Duration.ofHours(2);
            case "TYPE_2", "TI2" -> Duration.ofHours(2);
            case "TYPE_3", "TI3" -> Duration.ofHours(4);
            default -> Duration.ofHours(2);
        };
    }
    
    public Duration getWorkshopDuration() {
        return switch (type) {
            case "TYPE_1", "TI1" -> Duration.ZERO;
            case "TYPE_2", "TI2" -> Duration.ofHours(8); // 1 turn
            case "TYPE_3", "TI3" -> Duration.ofHours(24); // 1 day
            default -> Duration.ZERO;
        };
    }
    
    public void setWorkshopStartTime(LocalDateTime time) {
        this.workShopStartTime = time;
    }
}
