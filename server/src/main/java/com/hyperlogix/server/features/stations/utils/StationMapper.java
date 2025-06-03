package com.hyperlogix.server.features.stations.utils;

import com.hyperlogix.server.domain.Station;
import com.hyperlogix.server.features.stations.entity.StationEntity;

public class StationMapper {
  public static Station mapToDomain(StationEntity entity) {
    if (entity == null)
      return null;
    Station station = new Station();
    station.setId(entity.getId());
    station.setName(entity.getName());
    station.setLocation(entity.getLocation());
    station.setMaxCapacity(entity.getMaxCapacity());
    station.setMainStation(entity.isMainStation());
    return station;
  }

  public static StationEntity mapToEntity(Station station) {
    if (station == null)
      return null;
    StationEntity entity = new StationEntity();
    entity.setId(station.getId());
    entity.setName(station.getName());
    entity.setLocation(station.getLocation());
    entity.setMaxCapacity(station.getMaxCapacity());
    entity.setMainStation(station.isMainStation());
    return entity;
  }
}
