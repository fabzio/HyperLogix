package com.hyperlogix.server.services.incident;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hyperlogix.server.config.Constants;
import com.hyperlogix.server.domain.ActiveIncident;
import com.hyperlogix.server.domain.CompletedIncident;
import com.hyperlogix.server.domain.Incident;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Point;
import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.domain.Stop;
import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.domain.TruckState;
import com.hyperlogix.server.services.simulation.SimulationEngine;

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
    
    private final Map<String, ActiveIncident> activeIncidents = new ConcurrentHashMap<>();
    private final List<CompletedIncident> completedIncidents = new ArrayList<>();
    private final List<Incident> pendingIncidents = new ArrayList<>();

    public enum IncidentType {
      TYPE_1("TI1"), // Llanta ponchada - 2h inmovilización, continúa ruta original
      TYPE_2("TI2"), // Motor ahogado - 2h inmovilización + 1 turno completo en taller
      TYPE_3("TI3"); // Choque - 4h inmovilización + 1 día completo en taller

      private final String code;

      IncidentType(String code) {
        this.code = code;
      }

      public String getCode() {
        return code;
      }
    }    

    public IncidentManagement(List<Incident> initialIncidents) {
        if (initialIncidents != null) {
            this.pendingIncidents.addAll(initialIncidents);
        }
    }
    
    public void addNewIncidents(List<Incident> newIncidents) {
        if (newIncidents != null) {
            this.pendingIncidents.addAll(newIncidents);
            log.info("Added {} new pending incidents", newIncidents.size());
        }
    }

    // Generar incidentes para rutas (usado en planificación)
    public List<Incident> generateIncidentsForRoutes(Routes routes, String turn, LocalDateTime currentTime) {
        List<Incident> newIncidents = new ArrayList<>();
        
        for (Map.Entry<String, List<Stop>> entry : routes.getStops().entrySet()) {
            String truckCode = entry.getKey();
            List<Stop> routeStops = entry.getValue();
            
            // Solo generar incidente si el camión no tiene uno activo
            if (activeIncidents.containsKey(truckCode)) {
                continue;
            }
            
            Incident incident = generateIncidentForTruck(truckCode, routeStops, turn, currentTime);
            if (incident != null) {
                newIncidents.add(incident);
                log.info("Generated incident {} for truck {} at turn {}", 
                    incident.getType(), truckCode, turn);
            }
        }
        
        return newIncidents;
    }
    
    private Incident generateIncidentForTruck(String truckCode, List<Stop> routeStops, 
                                            String turn, LocalDateTime currentTime) {
        if (routeStops.isEmpty()) {
            return null;
        }
        
        // Verificar probabilidad del 30% para que ocurra un incidente
        if (random.nextDouble() > 0.30) {
            return null;
        }
        
        // Seleccionar un punto entre el 5% y 35% de la ruta
        int totalStops = routeStops.size();
        int minIndex = Math.max(1, (int) (totalStops * 0.05));
        int maxIndex = Math.min(totalStops - 1, (int) (totalStops * 0.35));
        
        if (minIndex >= maxIndex) {
            minIndex = 1;
            maxIndex = Math.max(2, totalStops - 1);
        }
        
        int incidentIndex = minIndex + random.nextInt(maxIndex - minIndex);
        Stop incidentStop = routeStops.get(incidentIndex);
        
        // Seleccionar tipo de incidente aleatoriamente
        IncidentType[] types = IncidentType.values();
        IncidentType selectedType = types[random.nextInt(types.length)];
        
        return new Incident(
            UUID.randomUUID().toString(),
            turn,
            selectedType.getCode(),
            truckCode,
            incidentStop.getNode().getLocation(),
            0  // daysSinceIncident
        );
    }

    // Procesar incidentes pendientes y activar los que correspondan
    public void processIncidents(PLGNetwork network, LocalDateTime currentTime) {
        //String currentTurn = getCurrentTurn(currentTime);
        
        // Verificar incidentes pendientes que deben activarse
        List<Incident> toActivate = new ArrayList<>();
        Iterator<Incident> pendingIterator = pendingIncidents.iterator();
        
        while (pendingIterator.hasNext()) {
            Incident incident = pendingIterator.next();
            
            // Verificar si el camión está en el lugar del incidente
            Truck truck = network.getTruckById(incident.getTruckCode());
            if (truck != null && isTruckAtIncidentLocation(truck, incident)) {
                toActivate.add(incident);
                pendingIterator.remove();
            }
        }
        
        // Activar incidentes
        for (Incident incident : toActivate) {
            activateIncident(incident, network, currentTime);
        }
        
        // Procesar incidentes activos
        processActiveIncidents(network, currentTime);
    }

    private void activateIncident(Incident incident, PLGNetwork network, LocalDateTime currentTime) {
        Truck truck = network.getTruckById(incident.getTruckCode());
        if (truck == null) return;

        ActiveIncident activeIncident = new ActiveIncident(
            incident.getId(),
            incident.getTruckCode(),
            incident.getTurn(),
            incident.getType(),
            incident.getLocation(),
            currentTime
        );

        activeIncidents.put(incident.getTruckCode(), activeIncident);
        
        // Aplicar efectos del incidente
        applyIncidentEffects(truck, activeIncident, currentTime);
        
        log.info("Activated incident {} for truck {} at location ({}, {})", 
            incident.getId(), incident.getTruckCode(), 
            incident.getLocation().x(), incident.getLocation().y());
    }

    private void applyIncidentEffects(Truck truck, ActiveIncident incident, LocalDateTime currentTime) {
        IncidentType type = determineIncidentType(incident.getType());
        
        switch (type) {
            case TYPE_1:
                // Llanta ponchada - 2h inmovilización en el lugar
                truck.setStatus(TruckState.BROKEN_DOWN);
                truck.setLocation(incident.getLocation());
                truck.setMaintenanceStartTime(currentTime);
                log.info("TYPE_1 INCIDENT - Flat tire: {} immobilized for 2 hours", truck.getCode());
                break;
                
            case TYPE_2:
                // Motor ahogado - 2h inmovilización + taller
                truck.setStatus(TruckState.BROKEN_DOWN);
                truck.setLocation(incident.getLocation());
                truck.setMaintenanceStartTime(currentTime);
                log.info("TYPE_2 INCIDENT - Engine failure: {} immobilized + workshop", truck.getCode());
                break;
                
            case TYPE_3:
                // Choque - 4h inmovilización + taller
                truck.setStatus(TruckState.BROKEN_DOWN);
                truck.setLocation(incident.getLocation());
                truck.setMaintenanceStartTime(currentTime);
                log.info("TYPE_3 INCIDENT - Crash: {} immobilized + workshop", truck.getCode());
                break;
        }
    }

    private void processActiveIncidents(PLGNetwork network, LocalDateTime currentTime) {
        List<String> completedIds = new ArrayList<>();
        
        for (ActiveIncident incident : activeIncidents.values()) {
            Truck truck = network.getTruckById(incident.getTruckId());
            if (truck == null) continue;
            
            // Verificar si terminó la inmovilización
            if (incident.isImmobilizationEnded(currentTime) && incident.getWorkShopStartTime() == null) {
                handleImmobilizationEnd(truck, incident, currentTime);
            }
            
            // Verificar si debe ir al taller
            if (incident.isImmobilizationEnded(currentTime) && 
                needsWorkshop(incident.getType()) && 
                incident.getWorkShopStartTime() == null) {
                sendToWorkshop(truck, incident, currentTime);
            }
            
            // Verificar si el incidente terminó completamente
            if (isIncidentCompleted(incident, currentTime)) {
                completeIncident(truck, incident, currentTime);
                completedIds.add(incident.getId());
            }
        }
        
        // Remover incidentes completados
        for (String id : completedIds) {
            activeIncidents.values().removeIf(incident -> incident.getId().equals(id));
        }
    }

    private void handleImmobilizationEnd(Truck truck, ActiveIncident incident, LocalDateTime currentTime) {
        IncidentType type = determineIncidentType(incident.getType());
        
        if (type == IncidentType.TYPE_1) {
            // TYPE_1: Continúa con la ruta original
            truck.setStatus(TruckState.ACTIVE);
            truck.setMaintenanceStartTime(null);
            log.info("Truck {} recovered from TYPE_1 incident, continuing route", truck.getCode());
        } else {
            // TYPE_2 y TYPE_3: Deben ir al taller
            sendToWorkshop(truck, incident, currentTime);
        }
    }

    private void sendToWorkshop(Truck truck, ActiveIncident incident, LocalDateTime currentTime) {
        // Enviar al almacén (taller)
        truck.setStatus(TruckState.MAINTENANCE);
        incident.setWorkshopStartTime(currentTime);
        
        // Aquí podrías mover el truck al almacén si tienes las coordenadas
        // truck.setLocation(warehouseLocation);
        
        log.info("Truck {} sent to workshop for {} incident", truck.getCode(), incident.getType());
    }

    private void completeIncident(Truck truck, ActiveIncident incident, LocalDateTime currentTime) {
        // Truck disponible nuevamente
        truck.setStatus(TruckState.IDLE);
        truck.setMaintenanceStartTime(null);
        
        // Crear incidente completado
        CompletedIncident completed = new CompletedIncident(incident, currentTime);
        completedIncidents.add(completed);
        
        log.info("Incident {} completed for truck {} at {}", 
            incident.getId(), truck.getCode(), currentTime);
    }

    private boolean needsWorkshop(String type) {
        return "TYPE_2".equals(type) || "TI2".equals(type) || 
               "TYPE_3".equals(type) || "TI3".equals(type);
    }

    private boolean isIncidentCompleted(ActiveIncident incident, LocalDateTime currentTime) {
        IncidentType type = determineIncidentType(incident.getType());
        
        return switch (type) {
            case TYPE_1 -> incident.isImmobilizationEnded(currentTime);
            case TYPE_2, TYPE_3 -> incident.getWorkShopStartTime() != null && 
                                  incident.isWorkshopEnded(currentTime);
        };
    }

    private boolean isTruckAtIncidentLocation(Truck truck, Incident incident) {
        Point truckLocation = truck.getLocation();
        Point incidentLocation = incident.getLocation();
        
        double distance = Math.sqrt(
            Math.pow(truckLocation.x() - incidentLocation.x(), 2) + 
            Math.pow(truckLocation.y() - incidentLocation.y(), 2)
        );
        
        return distance <= 50.0;
    }

    private IncidentType determineIncidentType(String typeStr) {
        String type = typeStr.toUpperCase();
        
        if (type.contains("TI1") || type.equals("TYPE_1")) {
            return IncidentType.TYPE_1;
        } else if (type.contains("TI2") || type.equals("TYPE_2")) {
            return IncidentType.TYPE_2;
        } else if (type.contains("TI3") || type.equals("TYPE_3")) {
            return IncidentType.TYPE_3;
        }
        
        return IncidentType.TYPE_1;
    }

    private String getCurrentTurn(LocalDateTime dateTime) {
        int hour = dateTime.getHour() % 24;
        if (hour >= 0 && hour < 8) {
            return "T1";
        } else if (hour >= 8 && hour < 16) {
            return "T2";
        } else {
            return "T3";
        }
    }

    public Map<String, ActiveIncident> getActiveIncidents() {
        return new HashMap<>(activeIncidents);
    }

    public List<CompletedIncident> getCompletedIncidents() {
        return new ArrayList<>(completedIncidents);
    }

    public List<Incident> getPendingIncidents() {
        return new ArrayList<>(pendingIncidents);
    }

    // Métodos de utilidad
    public boolean hasPendingIncidents() {
        return !pendingIncidents.isEmpty();
    }

    public boolean hasActiveIncidents() {
        return !activeIncidents.isEmpty();
    }

    public ActiveIncident getActiveIncidentForTruck(String truckId) {
        return activeIncidents.get(truckId);
    }

  public boolean checkAndHandleIncident(Truck truck, LocalDateTime simulatedTime) {
    String currentTurn = getCurrentTurn(simulatedTime);
    
    // Buscar incidente que corresponda al código del camión y al turno actual
    Incident targetIncident = null;
    for (Incident incident : pendingIncidents) {
      if (incident.getTruckCode().equals(truck.getCode()) && 
          incident.getTurn().equals(currentTurn)) {
        targetIncident = incident;
        break;
      }
    }
    
    if (targetIncident != null) {
      // Verificar si el camión está en el punto de la ruta donde debe ocurrir el incidente
      if (isTruckAtIncidentLocation(truck, targetIncident)) {
        //log.info("Incident triggered for truck {} during turn {} at route progress {}%", 
        //         truck.getCode(), currentTurn, (targetIncident.getRouteIndex() * 100.0 / 20));
        
        // Gestionar el incidente
        handleIncidentWithManagement(truck, targetIncident, simulatedTime);
        
        // Eliminar el incidente de la lista para evitar repeticiones
        pendingIncidents.remove(targetIncident);
        
        log.info("Incident removed from incidents list to prevent repetition");
        return true;
      }
    }
    return false;
  }

public CompletedIncident completeIncident(ActiveIncident activeIncident, LocalDateTime completionTime) {
    return new CompletedIncident(activeIncident, completionTime);
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
    applySpecificIncident(truck, incidentType, incident, simulatedTime);
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
    
    if (incidentTypeStr.contains("TI1") || incidentTypeStr.equals("TYPE_1")) {
      return IncidentType.TYPE_1;
    } else if (incidentTypeStr.contains("TI2") || incidentTypeStr.equals("TYPE_2")) {
      return IncidentType.TYPE_2;
    } else if (incidentTypeStr.contains("TI3") || incidentTypeStr.equals("TYPE_3")) {
      return IncidentType.TYPE_3;
    }
    
    // Por defecto, usar TYPE_1 (menos severo)
    return IncidentManagement.IncidentType.TYPE_1;
  }

  /**
   * Aplica un incidente específico a un camión
   * (Adaptado de IncidentManagement)
   */
  private void applySpecificIncident(Truck truck, IncidentManagement.IncidentType incidentType, Incident incident , LocalDateTime simulatedTime) {
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
        truck.setMaintenanceStartTime(simulatedTime);
        log.info("TYPE_1 INCIDENT - Flat tire: {} immobilized for 2 hours at location ({}, {})", 
                 truck.getCode(), incidentLocation.x(), incidentLocation.y());
        break;
      case TYPE_2:
        // TI2: Motor ahogado - 2h inmovilización + 1 turno completo en taller
        truck.setStatus(TruckState.BROKEN_DOWN); // Requiere taller
        truck.setLocation(incidentLocation);
        truck.setMaintenanceStartTime(simulatedTime);
        log.info("TYPE_2 INCIDENT - Engine failure: {} immobilized for 2 hours + 1 full turn in workshop at location ({}, {})", 
                 truck.getCode(), incidentLocation.x(), incidentLocation.y());
        break;
      case TYPE_3:
        // TI3: Choque - 4h inmovilización + 1 día completo en taller
        truck.setStatus(TruckState.BROKEN_DOWN); // Requiere taller
        truck.setLocation(incidentLocation);
        truck.setMaintenanceStartTime(simulatedTime);
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
