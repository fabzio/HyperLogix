package com.hyperlogix.server.features.orders.entity;

import com.hyperlogix.server.domain.OrderStatus;
import com.hyperlogix.server.domain.Point;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderEntity {
    @Id
    private String id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(nullable = false)
    private LocalDateTime date;

    @Embedded
    private Point location;

    @Column(name = "requested_glp", nullable = false)
    private int requestedGLP;

    @Column(name = "delivered_glp", nullable = false)
    private int deliveredGLP;

    @Column(name = "delivery_limit", nullable = false)
    private Duration deliveryLimit;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
}