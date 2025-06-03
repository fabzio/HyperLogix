package com.hyperlogix.server.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors; // Import Collectors

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

  public List<Order> getCalculatedOrders() {
    return orders.stream()
        .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
        .collect(Collectors.toList());
  }

  @Override
  public PLGNetwork clone() {
    try {
      PLGNetwork cloned = (PLGNetwork) super.clone();
      cloned.trucks = this.trucks.stream().map(Truck::clone).collect(Collectors.toList());
      cloned.stations = this.stations.stream().map(Station::clone).collect(Collectors.toList());
      cloned.orders = this.orders.stream().map(Order::clone).collect(Collectors.toList());
      cloned.incidents = List.copyOf(this.incidents);
      cloned.roadblocks = List.copyOf(this.roadblocks);
      return cloned;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError(); // Can't happen
    }
  }
}
