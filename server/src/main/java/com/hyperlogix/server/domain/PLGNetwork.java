package com.hyperlogix.server.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PLGNetwork implements Cloneable {
  List<Truck> trucks;
  List<Station> stations;
  List<Order> orders;
  List<Incident> incidents;
  List<Roadblock> roadblocks;

  public int getTrucksCapacity() {
    return trucks.stream().mapToInt(Truck::getMaxCapacity).sum();
  }

  @Override
  public PLGNetwork clone() {
    try {
      PLGNetwork cloned = (PLGNetwork) super.clone();
      cloned.trucks = List.copyOf(this.trucks);
      cloned.stations = List.copyOf(this.stations);
      cloned.orders = List.copyOf(this.orders);
      cloned.incidents = List.copyOf(this.incidents);
      cloned.roadblocks = List.copyOf(this.roadblocks);
      return cloned;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError(); // Can't happen
    }
  }
}
