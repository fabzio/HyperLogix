package com.hyperlogix.server.features.stations.entity;

import com.hyperlogix.server.domain.Point;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stations")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StationEntity {
  @Id
  private String id;

  @Column(nullable = false)
  private String name;

  @Embedded
  private Point location;

  @Column(nullable = false)
  private int maxCapacity = 160;

  @Column(nullable = false)
  private boolean mainStation = false;
}
