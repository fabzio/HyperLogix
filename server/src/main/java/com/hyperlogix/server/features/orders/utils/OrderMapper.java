package com.hyperlogix.server.features.orders.utils;

import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.features.orders.entity.OrderEntity;

public class OrderMapper {
  public static Order mapToDomain(OrderEntity entity) {
    if (entity == null)
      return null;
    Order order = new Order(entity.getId(), entity.getClientId(), entity.getDate(), entity.getLocation(),
        entity.getRequestedGLP(), entity.getDeliveredGLP(), entity.getDeliveryLimit(), entity.getStatus());
    return order;
  }

  public static OrderEntity mapToEntity(Order station) {
    if (station == null)
      return null;
    OrderEntity entity = new OrderEntity();
    entity.setId(station.getId());
    entity.setClientId(station.getClientId());
    entity.setDate(station.getDate());
    entity.setLocation(station.getLocation());
    entity.setRequestedGLP(station.getRequestedGLP());
    entity.setDeliveredGLP(station.getDeliveredGLP());
    entity.setDeliveryLimit(station.getDeliveryLimit());
    entity.setStatus(station.getStatus());

    return entity;
  }
}
