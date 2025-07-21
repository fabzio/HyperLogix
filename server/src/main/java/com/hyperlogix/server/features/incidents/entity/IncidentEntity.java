package com.hyperlogix.server.features.incidents.entity;

import com.hyperlogix.server.domain.IncidentType;
import com.hyperlogix.server.domain.Point;
import com.hyperlogix.server.features.trucks.entity.TruckEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "incidents")
@Data
@AllArgsConstructor
@NoArgsConstructor 
public class IncidentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turn", nullable = false)
    private String turn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_code", referencedColumnName = "code", nullable = false)
    private TruckEntity truck;    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private IncidentType type;
}