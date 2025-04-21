package com.hyperlogix.server.domain;

import com.hyperlogix.server.config.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Representa un camión en el sistema.
 */
@AllArgsConstructor
@Data
public class Truck {
  /**
   * Identificador único del camión.
   */
  private String id;
  /**
   * Código del camión, TT (Tipo) y NN (Correlativo).
   */
  private String code;
  /**
   * Tipo de camión.
   */
  private TruckType type;
  /**
   * Estado del camión.
   */
  private TruckState status = TruckState.ACTIVE;
  /**
   * Peso bruto en toneladas.
   */
  private double tareWeight;
  /**
   * Capacidad máxima en m³.
   */
  private int maxCapacity;

  /**
   * Capacidad actual en m³.
   */
  private int currentCapacity;
  /**
   * Capacidad máxima de combustible en galones.
   */
  private double fuelCapacity;
  /**
   * Capacidad actual de combustible en galones.
   */
  private double currentFuel;
  /**
   * Próxima fecha de mantenimiento.
   */
  private LocalDateTime nextMaintenance;
  /**
   * Ubicación del camión en el mapa.
   */
  private Point location;

  /**
   * @return Peso neto del camión en toneladas.
   */
  private double totalWeight() {
    return this.tareWeight + this.currentCapacity * Constants.GLP_WEIGHT;
  }

  /**
   * @param distance Distancia recorrida en km.
   * @return Consumo de combustible en galones.
   */
  public double getFuelConsumption(int distance) {
    return distance * totalWeight() / 180;
  }

  /**
   * @Params distance Distancia recorrida en km.
   */
  public Duration getTimeToDestination(int distance) {
    double hours = distance / Constants.TRUCK_SPEED;
    return Duration.ofHours((long) hours);
  }

  /**
   * Entrar en mantenimiento.
   */
  public void startMaintenance() {
    this.status = TruckState.MAINTENANCE;
  }

  /**
   * Salir de mantenimiento.
   */
  public void endMaintenance() {
    this.status = TruckState.ACTIVE;
    this.nextMaintenance = LocalDateTime.now().plus(Constants.MAINTENANCE_TRUCK_PERIOD);
  }
}
