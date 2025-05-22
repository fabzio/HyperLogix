package com.hyperlogix.server.features.trucks.entity;

import com.hyperlogix.server.domain.TruckState;
import com.hyperlogix.server.domain.TruckType;
import com.hyperlogix.server.domain.Point;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "trucks")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TruckEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TruckType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TruckState status = TruckState.ACTIVE;

  @Column(nullable = false)
  private double tareWeight;

  @Column(nullable = false)
  private int maxCapacity;

  @Column(nullable = false)
  private int currentCapacity;

  @Column(nullable = false)
  private double fuelCapacity;

  @Column(nullable = false)
  private double currentFuel;

  private LocalDateTime nextMaintenance;

  @Embedded
  private Point location;
}
