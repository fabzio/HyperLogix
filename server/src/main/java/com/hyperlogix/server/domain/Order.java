package com.hyperlogix.server.domain;

import com.hyperlogix.server.config.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Duration;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class Order implements Cloneable {
  private String id;
  private String clientId;
  private LocalDateTime date;
  private Point location;
  private int requestedGLP;
  private int deliveredGLP;
  private Duration deliveryLimit;
  private OrderStatus status;
  private LocalDateTime blockEndTime; // Tiempo en que termina el bloqueo que afecta esta orden

  /**
   * @return Fecha máxima de entrega
   */
  public LocalDateTime getMaxDeliveryDate() {
    return date.plus(deliveryLimit);
  }

  /**
   * @return Fecha mínima de entrega
   */
  public LocalDateTime getMinDeliveryDate() {
    return date.plus(Constants.MIN_DELIVERY_TIME);
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