package com.hyperlogix.server.domain;

import com.hyperlogix.server.config.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class Order implements Cloneable { // Implement Cloneable
  /**
   * Código del pedido
   */
  String id;
  /**
   * Código del cliente
   */
  String clientId;
  /**
   * Fecha de llegada del pedido
   */
  private LocalDateTime date;
  /**
   * Ubicación de la entrega
   */
  Point location;
  /**
   * Cantidad GLP solicitada en m³
   */
  int requestedGLP;
  /**
   * Cantidad de GLP entregada en m³
   */
  int deliveredGLP;
  /**
   * Tiempo de llegada en horas
   */
  Duration duration;


  /**
   * @return Fecha máxima de entrega
   */
  public LocalDateTime getMaxDeliveryDate() {
    return date.plus(duration);
  }

  /**
   *
   * @return Fecha mínima de entrega
   */
  public LocalDateTime getMinDeliveryDate() {
    return date.minus(Constants.MIN_DELIVERY_TIME);
  }

  // Add clone method using the copy constructor
  @Override
  public Order clone() {
    try {
      Order cloned = (Order) super.clone();
      return cloned;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError(); // Can't happen
    }
  }
}
