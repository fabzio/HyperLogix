package com.hyperlogix.server.domain;

import com.hyperlogix.server.config.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Representa un camión en el sistema.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
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
  private double currentFuel;  /**
   * Próxima fecha de mantenimiento.
   */
  private LocalDateTime nextMaintenance;
  /**
   * Hora de inicio del mantenimiento actual (para incidentes TYPE_1).
   */
  private LocalDateTime maintenanceStartTime;
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
  public double getFuelConsumption(double distance) {
    return distance * totalWeight() / 180;
  }

  /**
   * @Params distance Distancia recorrida en km.
   */
  public Duration getTimeToDestination(int distance) {
    double hours = distance / Constants.TRUCK_SPEED;
    long minutes = Math.round(hours * 60);
    return Duration.ofMinutes(minutes);
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

  /**
   * Verifica la recuperación de un incidente para el camión (sin usar fecha/hora)
   * Simula la lógica de recuperación según el tipo y turno del incidente.   * Se debe llamar una vez por cada turno simulado.
   * El método asume que el camión puede estar en uno de estos estados:
   * - BROKEN_DOWN: inmovilizado en el nodo
   * - MAINTENANCE: en taller
   * - ACTIVE: disponible
   */
  public void checkIncidentRecovery(Incident incident, String currentTurn) {
    if (!this.code.equals(incident.getTruckCode())) {
        return;
    }
    String type = incident.getType();
    String incidentTurn = incident.getTurn();
    int daysSinceIncident = incident.getDaysSinceIncident();

    // Tipo 1: inmovilizado 2h (simulamos como BROKEN_DOWN solo el turno del incidente)
    if (type.equals("TI1")) {
        // Solo está inmovilizado en el turno del incidente
        this.status = currentTurn.equals(incidentTurn) && daysSinceIncident == 0 ? TruckState.BROKEN_DOWN : TruckState.ACTIVE;
        return;
    }

    // Tipo 2: inmovilizado 2h + 1 turno completo en taller
    if (type.equals("TI2")) {
        // Inmovilizado en el turno del incidente
        if (currentTurn.equals(incidentTurn) && incident.getDaysSinceIncident() == 0) {
            this.status = TruckState.BROKEN_DOWN;
            return;
        }
        // En taller según reglas
        boolean inMaintenance = false;
        if (incidentTurn.equals("T1")) {
            // Disponible en T3 del mismo día
            inMaintenance = (incident.getDaysSinceIncident() == 0 && (currentTurn.equals("T2"))) ||
                            (incident.getDaysSinceIncident() == 0 && (currentTurn.equals("T3")));
        } else if (incidentTurn.equals("T2")) {
            // Disponible en T1 del día siguiente
            inMaintenance = (incident.getDaysSinceIncident() == 0 && currentTurn.equals("T3")) ||
                            (incident.getDaysSinceIncident() == 1 && currentTurn.equals("T1"));
        } else if (incidentTurn.equals("T3")) {
            // Disponible en T2 del día siguiente
            inMaintenance = (incident.getDaysSinceIncident() == 1 && (currentTurn.equals("T1") || currentTurn.equals("T2")));
        }
        this.status = inMaintenance ? TruckState.MAINTENANCE : TruckState.ACTIVE;
        return;
    }

    // Tipo 3: inmovilizado 4h + 1 día completo en taller (disponible en T1 del día A+3)
    if (type.equals("TI3")) {
        // Inmovilizado en el turno del incidente
        if (currentTurn.equals(incidentTurn) && incident.getDaysSinceIncident() == 0) {
            this.status = TruckState.BROKEN_DOWN;
            return;
        }
        // En taller hasta el día A+3, T1
        if (incident.getDaysSinceIncident() < 3 || (incident.getDaysSinceIncident() == 3 && !currentTurn.equals("T1"))) {
            this.status = TruckState.MAINTENANCE;
            return;
        }
        // Disponible a partir de T1 del día A+3
        this.status = TruckState.ACTIVE;
        return;
    }

    // Por defecto, activo
    this.status = TruckState.ACTIVE;
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
