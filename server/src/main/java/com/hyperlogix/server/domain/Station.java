package com.hyperlogix.server.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor; // Add NoArgsConstructor for copy constructor if needed

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap; // Import HashMap
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor // May be needed if using a manual copy constructor
public class Station implements Cloneable { // Implement Cloneable
  private String id;
  private String name;
  private Point location;
  private int maxCapacity = 160;
  private boolean mainStation = false;
  private Map<LocalDate, Integer> availableCapacityPerDate;

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
  }

  public void reserveCapacity(LocalDateTime dateTime, int amount) {
    LocalDate date = dateTime.toLocalDate();
    availableCapacityPerDate.putIfAbsent(date, maxCapacity);
    int availableCapacity = availableCapacityPerDate.get(date);
    if (availableCapacity >= amount) {
      availableCapacityPerDate.put(date, availableCapacity - amount);
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
      return cloned;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError(); // Can't happen
    }
  }
}