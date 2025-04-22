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
public class Truck implements Cloneable { // Implement Cloneable
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

  // Add clone method using super.clone()
  @Override
  public Truck clone() {
    try {
      // Perform a shallow copy first using super.clone()
      Truck cloned = (Truck) super.clone();
      // Since all fields (String, enums, primitives, LocalDateTime, Point record)
      // are effectively immutable or primitive, the shallow copy from super.clone()
      // results in a functionally deep copy for this class.
      // No need to manually copy fields here.
      return cloned;
    } catch (CloneNotSupportedException e) {
      // This should not happen since we implement Cloneable
      throw new AssertionError("Cloning failed for Truck", e);
    }
  }
}
