package com.hyperlogix.server.services.incident;

import com.hyperlogix.server.config.Constants;
import com.hyperlogix.server.domain.*;
import com.hyperlogix.server.services.simulation.SimulationEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Clase dedicada al manejo de incidencias en el sistema logístico.
 * Encapsula toda la lógica relacionada con la aplicación de incidentes
 * a las rutas de los camiones después de la optimización.
 * 
 * Esta clase maneja:
 * - Aplicación de incidentes a la mejor ruta encontrada
 * - Recuperación de camiones después de incidentes TYPE_1
 * - Cálculo de penalizaciones y retrasos por incidentes
 * - Gestión de estados de camiones durante incidentes
 */
public class IncidentManagement {
  private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);
  private final Random random = new Random();

  private List<Incident> incidents;

  public IncidentManagement(List<Incident> incidents) {
    // Create a mutable copy to allow removal of incidents
    this.incidents = new ArrayList<>(incidents);
  }

  private String getCurrentTurn(LocalDateTime arrivalTime) {
    int hour = arrivalTime.getHour() % 24; // Normalizar a rango 0-23
    if (hour >= 0 && hour < 8) {
      return "T1";
    } else if (hour >= 8 && hour < 16) {
      return "T2";
    } else {
      return "T3";
    }
  }

  /**
   * Verifica si debe ocurrir un incidente para el camión en el progreso actual
   * del path
   * 
   * @return El incidente detectado y manejado, o null si no se detectó ningún
   *         incidente
   */
  public Incident checkAndHandleIncident(Truck truck, LocalDateTime simulatedTime) {

    String currentTurn = getCurrentTurn(simulatedTime);

    // Buscar incidente que corresponda al código del camión y al turno actual
    Incident targetIncident = null;
    for (Incident incident : incidents) {
      if (incident.getTruckCode().equals(truck.getCode()) &&
          incident.getTurn().equals(currentTurn)) {
        targetIncident = incident;
        targetIncident.setDaysSinceIncident(0);
        targetIncident.setStatus(IncidentStatus.IMMOBILIZED);
        targetIncident.setIncidentTime(simulatedTime);
        break;
      }
    }

    if (targetIncident != null) {
      // Verificar probabilidad del 30% antes de aplicar el incidente
      double probabilityRoll = random.nextDouble();
      if (probabilityRoll <= 0.30) {
        log.info(
            "Incident triggered for truck {} during turn {} at progress between 5%-35% of current path (probability: {:.3f})",
            truck.getCode(), currentTurn, probabilityRoll);

        // Gestionar el incidente usando IncidentManagement
        handleIncidentWithManagement(truck, targetIncident, simulatedTime);

        // Eliminar el incidente de la lista para evitar repeticiones
        incidents.remove(targetIncident);

        log.info("Incident removed from plgNetwork to prevent repetition");
        return targetIncident; // Retorna el incidente que se manejó
      } else {
        log.debug("Incident available for truck {} but probability check failed (rolled {:.3f}, needed <= 0.300)",
            truck.getCode(), probabilityRoll);
      }
    }
    return null; // Retorna null si no se manejó un incidente
  }

  /**
   * Maneja un incidente usando las funciones encapsuladas de IncidentManagement
   */
  public void handleIncidentWithManagement(Truck truck, Incident incident, LocalDateTime simulatedTime) {

    // Determinar el tipo de incidente usando el método de IncidentManagement
    IncidentType incidentType = determineIncidentType(incident, simulatedTime);

    // Aplicar el incidente específico según su tipo

    applySpecificIncident(truck, incident);

  }

  /**
   * Determina el tipo de incidente basado en el objeto Incident del sistema
   * (Adaptado de IncidentManagement)
   */
  private IncidentType determineIncidentType(Incident incident, LocalDateTime simulatedTime) {
    String incidentTypeStr = incident.getType().toString();

    if (incidentTypeStr.contains("TI1") || incidentTypeStr.equals("1")) {
      incident.setExpectedRecovery(simulatedTime.plusHours(2)); // 2 horas de inmovilización
      return IncidentType.TI1;
    } else if (incidentTypeStr.contains("TI2") || incidentTypeStr.equals("2")) {
      incident.setExpectedRecovery(simulatedTime.plusHours(2)); // 2 horas de inmovilización
      return IncidentType.TI2;
    } else if (incidentTypeStr.contains("TI3") || incidentTypeStr.equals("3")) {
      incident.setExpectedRecovery(simulatedTime.plusHours(4)); // 4 horas de inmovilización
      return IncidentType.TI3;
    }

    // Por defecto, usar TYPE_1 (menos severo)
    return IncidentType.TI1;
  }

  /**
   * Aplica un incidente específico a un camión
   * (Adaptado de IncidentManagement)
   */
  private void applySpecificIncident(Truck truck, Incident incident) {
    // Validar la ubicación del incidente (usar ubicación actual del camión)
    Point incidentLocation = validateIncidentLocation(truck.getLocation());

    // Actualizar la ubicación del incidente
    incident.setLocation(incidentLocation);
    incident.setFuel(truck.getCurrentCapacity());
    truck.setStatus(TruckState.BROKEN_DOWN);

  }

  /**
   * Valida que la ubicación del incidente esté dentro de los límites del mapa
   * (Adaptado de IncidentManagement)
   */
  private Point validateIncidentLocation(Point location) {
    double x = Math.max(0, Math.min(location.x(), Constants.MAP_WIDTH));
    double y = Math.max(0, Math.min(location.y(), Constants.MAP_HEIGHT));
    return new Point(x, y);
  }

  /**
   * Maneja el retraso de mantenimiento para incidentes TYPE_1 (llanta ponchada)
   * El camión permanece inmóvil en su posición actual por 2 horas
   * 
   * @return true if truck recovered from maintenance, false if still in
   *         maintenance
   */
  public boolean handleMaintenanceDelay(Truck truck, LocalDateTime simulatedTime, Incident incident) { // Solo procesar
                                                                                                       // si el camión
                                                                                                       // está en
                                                                                                       // mantenimiento
    if (truck.getStatus() != TruckState.BROKEN_DOWN) {
      return false; // Truck is not in maintenance
    }

    // Verificar si el camión tiene un tiempo de inicio de mantenimiento registrado
    LocalDateTime maintenanceStartTime = truck.getMaintenanceStartTime();

    if (maintenanceStartTime == null) {
      // Primera vez que se detecta el estado MAINTENANCE, registrar el tiempo de
      // inicio
      truck.setMaintenanceStartTime(simulatedTime);
      log.info("Truck {} started TYPE_1 maintenance at {} - will be immobilized for 2 hours at position ({}, {})",
          truck.getCode(), simulatedTime, truck.getLocation().x(), truck.getLocation().y());
      return false; // Truck just started maintenance
    }

    // Calcular tiempo transcurrido desde el inicio del mantenimiento
    Duration maintenanceElapsed = Duration.between(maintenanceStartTime, simulatedTime);
    long hoursElapsed = maintenanceElapsed.toHours();

    // Si han pasado 2 horas o más, reactivar el camión
    if ((hoursElapsed >= 2) && (incident.getType() == IncidentType.TI1)) {
      truck.setMaintenanceStartTime(null); // Limpiar el tiempo de inicio
      truck.setStatus(TruckState.IDLE);
      incident.setStatus(IncidentStatus.RESOLVED);
      return true; // Truck has recovered from maintenance
    } else if ((hoursElapsed >= 2) && (incident.getType() == IncidentType.TI2)) {
      truck.setMaintenanceStartTime(simulatedTime);
      truck.setStatus(TruckState.MAINTENANCE);
      truck.setLocation(new Point(12, 8));
      incident.setStatus(IncidentStatus.IN_MAINTENANCE);

      // Determinar el subsiguiente turno según el turno actual
      String currentTurn = getCurrentTurn(incident.getIncidentTime());
      LocalDateTime recoveryTime;

      if (currentTurn.equals("T1")) {
        // Si es turno 1 (00:00-08:00), estará disponible en turno 3 (16:00-00:00)
        recoveryTime = simulatedTime.withHour(16).withMinute(0).withSecond(0).withNano(0);
      } else if (currentTurn.equals("T2")) {
        // Si es turno 2 (08:00-16:00), estará disponible en turno 1 (00:00-08:00) del
        // día siguiente
        recoveryTime = simulatedTime.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
      } else { // T3
        // Si es turno 3 (16:00-00:00), estará disponible en turno 2 (08:00-16:00) del
        // día siguiente
        recoveryTime = simulatedTime.plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
      }
      incident.setExpectedRecovery(recoveryTime);
      return true; // Truck has recovered from maintenance
    } else if ((hoursElapsed >= 4) && (incident.getType() == IncidentType.TI3)) {
      truck.setMaintenanceStartTime(simulatedTime);
      truck.setStatus(TruckState.MAINTENANCE);
      truck.setLocation(new Point(12, 8));
      incident.setStatus(IncidentStatus.IN_MAINTENANCE);

      LocalDateTime recoveryTime = incident.getIncidentTime().plusDays(2) // Día A+2
          .withHour(0) // Inicio del turno 1 (00:00)
          .withMinute(0)
          .withSecond(0)
          .withNano(0);

      incident.setExpectedRecovery(recoveryTime);

      return true; // Truck has recovered from maintenance
    } else {
      return false; // Truck is still in maintenance
    }
  }
}
