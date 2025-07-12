package com.hyperlogix.server.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Station implements Cloneable {
  private String id;
  private String name;
  private Point location;
  private int maxCapacity = 160;
  private boolean mainStation = false;
  private Map<LocalDate, Integer> availableCapacityPerDate = new HashMap<>();
  private List<Reservation> reservationHistory = new ArrayList<>();

  // Copy Constructor
  public Station(Station other) {
    this.id = other.id;
    this.name = other.name;
    this.location = other.location;
    this.maxCapacity = other.maxCapacity;
    this.mainStation = other.mainStation;
    // Deep copy the map
    this.availableCapacityPerDate = other.availableCapacityPerDate != null
        ? new HashMap<>(other.availableCapacityPerDate)
        : new HashMap<>();
    // Deep copy the reservation history
    this.reservationHistory = other.reservationHistory != null
        ? new ArrayList<>(other.reservationHistory)
        : new ArrayList<>();
  }

  public void reserveCapacity(LocalDateTime dateTime, int amount) {
    reserveCapacity(dateTime, amount, null, null);
  }

  public void reserveCapacity(LocalDateTime dateTime, int amount, String vehicleId, String orderId) {
    LocalDate date = dateTime.toLocalDate();
    availableCapacityPerDate.putIfAbsent(date, maxCapacity);
    int availableCapacity = availableCapacityPerDate.get(date);
    if (availableCapacity >= amount) {
      availableCapacityPerDate.put(date, availableCapacity - amount);
      // Add reservation to history with traceability info
      reservationHistory.add(new Reservation(dateTime, amount, vehicleId, orderId));
    }
  }

  public int getAvailableCapacity(LocalDateTime dateTime) {
    LocalDate date = dateTime.toLocalDate();
    return availableCapacityPerDate.getOrDefault(date, maxCapacity);
  }

  // Add clone method using the copy constructor
  @Override
  public Station clone() {
    try {
      Station cloned = (Station) super.clone();
      cloned.availableCapacityPerDate = this.availableCapacityPerDate != null
          ? new HashMap<>(this.availableCapacityPerDate)
          : new HashMap<>();
      cloned.reservationHistory = this.reservationHistory != null
          ? new ArrayList<>(this.reservationHistory)
          : new ArrayList<>();
      return cloned;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError(); // Can't happen
    }
  }

  // Inner class for Reservation
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Reservation {
    private LocalDateTime dateTime;
    private int amount;
    private String vehicleId;
    private String orderId; // Optional, can be null if not related to a specific order

    // Constructor for backward compatibility (without traceability info)
    public Reservation(LocalDateTime dateTime, int amount) {
      this.dateTime = dateTime;
      this.amount = amount;
      this.vehicleId = null;
      this.orderId = null;
    }
  }
}