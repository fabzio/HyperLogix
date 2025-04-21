package com.hyperlogix.server.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
public class Station {
  private String id;
  private String name;
  private Point location;
  private int maxCapacity = 160;
  private boolean mainStation = false;
  private Map<LocalDate, Integer> availableCapacityPerDate;

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
}