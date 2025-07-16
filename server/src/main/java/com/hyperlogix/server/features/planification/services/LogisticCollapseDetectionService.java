package com.hyperlogix.server.features.planification.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.features.planification.dtos.LogisticCollapseEvent;

@Service
public class LogisticCollapseDetectionService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Detecta colapsos logísticos basado en métricas de rendimiento
     */
    public void analyzeLogisticPerformance(String sessionId, PLGNetwork network, Routes routes) {
        // Detectar colapso por saturación de rutas
        detectRouteSaturation(sessionId, network, routes);

        // Detectar colapso por exceso de pedidos pendientes
        detectOrderBacklog(sessionId, network);

        // Detectar colapso por tiempo de entrega excesivo
        detectDeliveryTimeCollapse(sessionId, network, routes);

        // Detectar colapso por falta de recursos
        detectResourceShortage(sessionId, network);
    }

    private void detectRouteSaturation(String sessionId, PLGNetwork network, Routes routes) {
        if (routes == null || routes.getStops() == null) return;

        // Calcular número de rutas activas
        int routeCount = routes.getStops().size();

        if (routeCount == 0) {
            publishCollapseEvent(sessionId, "NO_ROUTES",
                "No se pudieron generar rutas para la planificación",
                0.9, "Toda la red");
            return;
        }

        // Simplificado: si hay muy pocas rutas para muchos pedidos
        int orderCount = network.getOrders().size();
        if (orderCount > 0 && routeCount < orderCount * 0.1) {
            publishCollapseEvent(sessionId, "ROUTE_SATURATION",
                "Saturación crítica de rutas detectada",
                0.8, "Red de distribución");
        }
    }

    private void detectOrderBacklog(String sessionId, PLGNetwork network) {
        List<Order> orders = network.getOrders();
        if (orders == null) return;

        // Contar pedidos pendientes (simplificado)
        long pendingOrders = orders.size();

        if (pendingOrders > 100) { // Umbral configurable
            publishCollapseEvent(sessionId, "ORDER_BACKLOG",
                "Acumulación excesiva de pedidos pendientes: " + pendingOrders,
                0.7, "Sistema de pedidos");
        }
    }

    private void detectDeliveryTimeCollapse(String sessionId, PLGNetwork network, Routes routes) {
        // Simulación de detección de tiempo de entrega
        if (routes != null && routes.getStops() != null) {
            int routeCount = routes.getStops().size();
            int orderCount = network.getOrders().size();

            // Si hay muchos más pedidos que rutas, probablemente hay demoras
            if (orderCount > routeCount * 10) {
                publishCollapseEvent(sessionId, "DELIVERY_DELAY",
                    "Tiempos de entrega excesivamente largos detectados",
                    0.6, "Entregas");
            }
        }
    }

    private void detectResourceShortage(String sessionId, PLGNetwork network) {
        // Verificar disponibilidad de camiones
        if (network.getTrucks() == null || network.getTrucks().isEmpty()) {
            publishCollapseEvent(sessionId, "RESOURCE_SHORTAGE",
                "Falta de recursos de transporte disponibles",
                0.85, "Flota de vehículos");
        }
    }

    /**
     * Método para reportar colapsos detectados manualmente
     */
    public void reportManualCollapse(String sessionId, String collapseType,
                                   String description, double severityLevel, String affectedArea) {
        publishCollapseEvent(sessionId, collapseType, description, severityLevel, affectedArea);
    }

    private void publishCollapseEvent(String sessionId, String collapseType,
                                    String description, double severityLevel, String affectedArea) {
        System.out.println("Colapso logístico detectado en sesión " + sessionId + ": " + collapseType + " - " + description);

        LogisticCollapseEvent event = new LogisticCollapseEvent(
            sessionId,
            collapseType,
            description,
            LocalDateTime.now(),
            severityLevel,
            affectedArea
        );

        eventPublisher.publishEvent(event);
    }
}
