package com.hyperlogix.server.services.incident;

import com.hyperlogix.server.config.Constants;
import com.hyperlogix.server.domain.*;
import com.hyperlogix.server.services.simulation.SimulationEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Random;

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
    public enum IncidentType {
        TYPE_1, // Llanta ponchada - 2h inmovilización, continúa ruta original
        TYPE_2, // Motor ahogado - 2h inmovilización + 1 turno completo en taller
        TYPE_3  // Choque - 4h inmovilización + 1 día completo en taller
    }      public IncidentManagement(List<Incident> incidents) {
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
   * Verifica si debe ocurrir un incidente para el camión en el progreso actual del path
   * @return El incidente detectado y manejado, o null si no se detectó ningún incidente
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
        break;
      }
    }
    
    if (targetIncident != null) {
      // Verificar probabilidad del 30% antes de aplicar el incidente
      double probabilityRoll = random.nextDouble();
      if (probabilityRoll <= 0.30) {
        log.info("Incident triggered for truck {} during turn {} at progress between 5%-35% of current path (probability: {:.3f})", 
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
  private void handleIncidentWithManagement(Truck truck, Incident incident, LocalDateTime simulatedTime) {
    log.info("=== HANDLING INCIDENT WITH MANAGEMENT ===");
    log.info("Truck: {}", truck.getCode());
    log.info("Incident Type: {}", incident.getType());
    log.info("Incident Turn: {}", incident.getTurn());
    log.info("Truck Location: ({}, {})", truck.getLocation().x(), truck.getLocation().y());
    log.info("Current Time: {}", simulatedTime);
    
    // Determinar el tipo de incidente usando el método de IncidentManagement
    IncidentManagement.IncidentType incidentType = determineIncidentType(incident);
    
    // Aplicar el incidente específico según su tipo
    TruckState previousStatus = truck.getStatus();
    applySpecificIncident(truck, incidentType, incident);
    TruckState newStatus = truck.getStatus();
    
    log.info("Incident applied: Status changed from {} to {}", previousStatus, newStatus);
    log.info("=== INCIDENT HANDLING COMPLETED ===");
  }

  /**
   * Determina el tipo de incidente basado en el objeto Incident del sistema
   * (Adaptado de IncidentManagement)
   */
  private IncidentManagement.IncidentType determineIncidentType(Incident incident) {
    String incidentTypeStr = incident.getType().toUpperCase();
    
    if (incidentTypeStr.contains("TI1") || incidentTypeStr.equals("1")) {
      return IncidentManagement.IncidentType.TYPE_1;
    } else if (incidentTypeStr.contains("TI2") || incidentTypeStr.equals("2")) {
      return IncidentManagement.IncidentType.TYPE_2;
    } else if (incidentTypeStr.contains("TI3") || incidentTypeStr.equals("3")) {
      return IncidentManagement.IncidentType.TYPE_3;
    }
    
    // Por defecto, usar TYPE_1 (menos severo)
    return IncidentManagement.IncidentType.TYPE_1;
  }

  /**
   * Aplica un incidente específico a un camión
   * (Adaptado de IncidentManagement)
   */
  private void applySpecificIncident(Truck truck, IncidentManagement.IncidentType incidentType, Incident incident) {
    // Validar la ubicación del incidente (usar ubicación actual del camión)
    Point incidentLocation = validateIncidentLocation(truck.getLocation());
    
    // Actualizar la ubicación del incidente
    incident.setLocation(incidentLocation);
    
    // Cambiar el estado del camión según el tipo de incidente
    switch (incidentType) {
      case TYPE_1:
        // TI1: Llanta ponchada - 2h inmovilización, puede repararse en el lugar
        truck.setStatus(TruckState.MAINTENANCE);
        truck.setLocation(incidentLocation);
        log.info("TYPE_1 INCIDENT - Flat tire: {} immobilized for 2 hours at location ({}, {})", 
                 truck.getCode(), incidentLocation.x(), incidentLocation.y());
        break;
      case TYPE_2:
        // TI2: Motor ahogado - 2h inmovilización + 1 turno completo en taller
        truck.setStatus(TruckState.BROKEN_DOWN); // Requiere taller
        truck.setLocation(incidentLocation);
        log.info("TYPE_2 INCIDENT - Engine failure: {} immobilized for 2 hours + 1 full turn in workshop at location ({}, {})", 
                 truck.getCode(), incidentLocation.x(), incidentLocation.y());
        break;
      case TYPE_3:
        // TI3: Choque - 4h inmovilización + 1 día completo en taller
        truck.setStatus(TruckState.BROKEN_DOWN); // Requiere taller
        truck.setLocation(incidentLocation);
        log.info("TYPE_3 INCIDENT - Crash: {} immobilized for 4 hours + 1 full day in workshop at location ({}, {})", 
                 truck.getCode(), incidentLocation.x(), incidentLocation.y());
        break;
    }
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
   * @return true if truck recovered from maintenance, false if still in maintenance
   */
  public boolean handleMaintenanceDelay(Truck truck, LocalDateTime simulatedTime) {    // Solo procesar si el camión está en mantenimiento
    if (truck.getStatus() != TruckState.MAINTENANCE) {
      return false; // Truck is not in maintenance
    }
    
    // Verificar si el camión tiene un tiempo de inicio de mantenimiento registrado
    LocalDateTime maintenanceStartTime = truck.getMaintenanceStartTime();
    
    if (maintenanceStartTime == null) {
      // Primera vez que se detecta el estado MAINTENANCE, registrar el tiempo de inicio
      truck.setMaintenanceStartTime(simulatedTime);
      log.info("Truck {} started TYPE_1 maintenance at {} - will be immobilized for 2 hours at position ({}, {})", 
               truck.getCode(), simulatedTime, truck.getLocation().x(), truck.getLocation().y());
      return false; // Truck just started maintenance
    }
    
    // Calcular tiempo transcurrido desde el inicio del mantenimiento
    Duration maintenanceElapsed = Duration.between(maintenanceStartTime, simulatedTime);
    long hoursElapsed = maintenanceElapsed.toHours();
    
    // Si han pasado 2 horas o más, reactivar el camión
    if (hoursElapsed >= 2) {
      truck.setStatus(TruckState.ACTIVE);
      truck.setMaintenanceStartTime(null); // Limpiar el tiempo de inicio
        log.info("Truck {} maintenance completed after {} hours - reactivated and ready to continue at position ({}, {})", 
               truck.getCode(), hoursElapsed, truck.getLocation().x(), truck.getLocation().y());
      
      return true; // Truck has recovered from maintenance
    } else {
      // El camión sigue en mantenimiento, logear progreso
      long minutesRemaining = (2 * 60) - maintenanceElapsed.toMinutes();
      log.debug("Truck {} still in maintenance - {} minutes remaining (elapsed: {} minutes)", 
                truck.getCode(), minutesRemaining, maintenanceElapsed.toMinutes());
      
      return false; // Truck is still in maintenance
    }
  }
}
