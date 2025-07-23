package com.hyperlogix.server.optimizer.AntColony;

import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.domain.Stop;
import com.hyperlogix.server.domain.Node;
import com.hyperlogix.server.domain.NodeType;
import com.hyperlogix.server.domain.Path;
import com.hyperlogix.server.domain.TruckState;
import com.hyperlogix.server.domain.OrderStatus;
import com.hyperlogix.server.domain.Roadblock;
import com.hyperlogix.server.domain.Point;
import com.hyperlogix.server.optimizer.Graph;
import com.hyperlogix.server.optimizer.Notifier;
import com.hyperlogix.server.optimizer.Optimizer;
import com.hyperlogix.server.optimizer.OptimizerContext;
import com.hyperlogix.server.optimizer.OptimizerResult;
import com.hyperlogix.server.features.planification.dtos.LogisticCollapseEvent;

import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.*;

public class AntColonyOptimizer implements Optimizer {
  private AntColonyConfig antColonyConfig;
  private Graph graph;
  private List<Ant> ants;
  private ApplicationEventPublisher eventPublisher;
  private String sessionId;
  private OptimizerContext currentContext; // Para usar en la solución de emergencia

  public AntColonyOptimizer(AntColonyConfig antColonyConfig) {
    this.antColonyConfig = antColonyConfig;
  }

  public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  @Override
  public OptimizerResult run(OptimizerContext ctx, Duration maxDuration, Notifier notifier) {
    this.currentContext = ctx; // Guardar contexto para solución de emergencia
    graph = new Graph(ctx.plgNetwork, ctx.algorithmStartDate, antColonyConfig, ctx.incidents);

    ants = new ArrayList<>();
    for (int i = 0; i < antColonyConfig.NUM_ANTS(); i++) {
      Ant ant = new Ant(ctx.plgNetwork, graph, antColonyConfig, ctx.incidents);
      // Configurar el evento publisher y sessionId para cada hormiga
      ant.setEventPublisher(eventPublisher);
      ant.setSessionId(sessionId);
      
      // TEMPORAL: Agregar roadblocks simulados basados en patrones típicos
      ant.setRoadblocks(createSimulatedRoadblocks());
      
      ants.add(ant);
    }

    Routes bestSolution = null;
    long startTime = System.currentTimeMillis();
    long maxDurationMillis = maxDuration.toMillis();

    // Configuración híbrida: paralelismo controlado con lotes pequeños
    int batchSize = Math.min(4, ants.size()); // Máximo 4 hormigas por lote
    int numThreads = Math.min(batchSize, Runtime.getRuntime().availableProcessors());
    
    for (int iteration = 0; iteration < antColonyConfig.NUM_ITERATIONS(); iteration++) {
      long elapsedTime = System.currentTimeMillis() - startTime;
      
      // EARLY EXIT: Si hemos usado más del 80% del tiempo y tenemos una solución, salir
      if (elapsedTime >= maxDurationMillis * 0.85 && bestSolution != null) {
        System.out.println("Early exit at iteration " + iteration + " with valid solution to avoid timeout");
        break;
      }
      
      if (elapsedTime >= maxDurationMillis) {
        System.out.println("Optimization terminated due to time limit. Completed " + iteration + " iterations.");
        break;
      }

      ants.forEach(Ant::resetState);
      List<Routes> solutions = new ArrayList<>();

      // Procesar hormigas en lotes pequeños con timeout por lote
      for (int batchStart = 0; batchStart < ants.size(); batchStart += batchSize) {
        int batchEnd = Math.min(batchStart + batchSize, ants.size());
        List<Ant> currentBatch = ants.subList(batchStart, batchEnd);
        
        final int currentIteration = iteration;
        final int currentBatchStart = batchStart;
        
        ExecutorService batchExecutor = Executors.newFixedThreadPool(numThreads, r -> {
          Thread t = new Thread(r, "AntBatch-" + currentIteration + "-" + currentBatchStart);
          t.setDaemon(true);
          return t;
        });

        try {
          List<Future<Routes>> batchFutures = new ArrayList<>();
          
          for (Ant ant : currentBatch) {
            Future<Routes> future = batchExecutor.submit(() -> {
              try {
                return ant.findSolution();
              } catch (Exception e) {
                System.err.println("Ant execution error: " + e.getMessage());
                return null; // Retornar null en caso de error
              }
            });
            batchFutures.add(future);
          }

          // Esperar resultados del lote con timeout más corto
          for (Future<Routes> future : batchFutures) {
            try {
              Routes result = future.get(30, TimeUnit.SECONDS); // Timeout de 30 segundos por hormiga
              if (result != null) {
                solutions.add(result);
              }
            } catch (TimeoutException e) {
              System.err.println("Ant execution timeout, cancelling...");
              future.cancel(true);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              System.err.println("Batch interrupted: " + e.getMessage());
              publishLogisticCollapseEvent("ALGORITHM_INTERRUPTION",
                  "Lote de hormigas interrumpido durante optimización", 7.0, "OPTIMIZATION_ENGINE");
              break;
            } catch (ExecutionException e) {
              System.err.println("Ant execution failed: " + e.getMessage());
              // Continuar con las otras hormigas del lote
            }
          }
        } finally {
          // Cierre rápido y forzado del executor del lote
          batchExecutor.shutdownNow();
          try {
            if (!batchExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
              System.err.println("Batch executor did not terminate quickly");
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }

        // Verificar si debemos terminar por tiempo
        elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime >= maxDurationMillis) {
          System.out.println("Time limit reached during batch processing.");
          break;
        }
      }

      // Procesar resultados de la iteración
      if (!solutions.isEmpty()) {
        List<Routes> sortedSolutions = new ArrayList<>(solutions);
        sortedSolutions.sort((r1, r2) -> Double.compare(r1.getCost(), r2.getCost()));

        if (bestSolution == null || sortedSolutions.get(0).getCost() < bestSolution.getCost()) {
          bestSolution = sortedSolutions.get(0);
        }
        graph.updatePheromoneMap(sortedSolutions, antColonyConfig);
        
        // EARLY EXIT: Si encontramos la primera solución válida, registrarla
        if (bestSolution != null && iteration == 0) {
          System.out.println("Found first valid solution in iteration " + iteration + ", continuing to optimize...");
        }
      } else {
        System.err.println("No valid solutions found in iteration " + iteration);
      }

      if (notifier != null) {
        notifier.notify(new OptimizerResult(
            bestSolution,
            bestSolution != null ? bestSolution.getCost() : Double.MAX_VALUE));
      }
    }

    // Si no se encontró solución válida dentro del tiempo límite, crear solución de emergencia
    if (bestSolution == null) {
      System.out.println("No solution found through normal optimization, attempting emergency solution...");
      bestSolution = createEmergencySolution();
      
      if (eventPublisher != null && sessionId != null) {
        LogisticCollapseEvent collapseEvent = new LogisticCollapseEvent(
            sessionId,
            "EMERGENCY_SOLUTION_ACTIVATED",
            "Se activó solución de emergencia debido a falta de solución válida en " + maxDuration.toMinutes() + " minutos",
            java.time.LocalDateTime.now(),
            0.7, // Menor severidad ya que tenemos una solución
            "Algoritmo de optimización");
        eventPublisher.publishEvent(collapseEvent);
      }
    }

    // Return best solution found (incluyendo solución de emergencia)
    if (bestSolution != null) {
      return new OptimizerResult(bestSolution, bestSolution.getCost());
    } else {
      return new OptimizerResult(null, Double.MAX_VALUE);
    }
  }

  @Override
  public OptimizerResult run(OptimizerContext ctx, Duration maxDuration) {
    return run(ctx, maxDuration, null);
  }

  /**
   * Publica un evento de colapso logístico cuando ocurre una InterruptedException
   */
  private void publishLogisticCollapseEvent(String collapseType, String description,
                                          double severityLevel, String affectedArea) {
    if (eventPublisher != null) {
      LogisticCollapseEvent collapseEvent = new LogisticCollapseEvent(
          sessionId != null ? sessionId : "UNKNOWN_SESSION",
          collapseType,
          description,
          LocalDateTime.now(),
          severityLevel,
          affectedArea
      );

      eventPublisher.publishEvent(collapseEvent);
      System.err.println("COLAPSO LOGÍSTICO DETECTADO: " + collapseType + " - " + description);
    }
  }

  /**
   * Crea una solución de emergencia cuando el algoritmo normal falla
   */
  private Routes createEmergencySolution() {
    System.out.println("Creating emergency solution due to algorithm timeout");
    
    Map<String, List<Stop>> emergencyStops = new HashMap<>();
    Map<String, List<Path>> emergencyPaths = new HashMap<>();
    
    // Asignar órdenes de manera simple: primer camión disponible toma la primera orden
    List<Order> calculatingOrders = currentContext.plgNetwork.getOrders().stream()
        .filter(order -> order.getStatus() == OrderStatus.CALCULATING)
        .toList();
        
    List<Truck> availableTrucks = currentContext.plgNetwork.getTrucks().stream()
        .filter(truck -> truck.getStatus() == TruckState.IDLE)
        .toList();
    
    System.out.println("Emergency solution: " + calculatingOrders.size() + " orders, " + availableTrucks.size() + " trucks");
    
    for (int i = 0; i < Math.min(calculatingOrders.size(), availableTrucks.size()); i++) {
        Order order = calculatingOrders.get(i);
        Truck truck = availableTrucks.get(i);
        
        // Crear ruta simple: ubicación inicial -> entrega
        List<Stop> stops = new ArrayList<>();
        
        // Parada inicial
        stops.add(new Stop(
            new Node(truck.getCode(), truck.getType().toString(), 
                    NodeType.LOCATION, truck.getLocation().integerPoint()),
            currentContext.algorithmStartDate
        ));
        
        // Parada de entrega
        stops.add(new Stop(
            new Node(order.getId(), "Emergency-Delivery", 
                    NodeType.DELIVERY, order.getLocation().integerPoint()),
            currentContext.algorithmStartDate.plusMinutes(30) // Estimación simple
        ));
        
        emergencyStops.put(truck.getId(), stops);
        emergencyPaths.put(truck.getId(), new ArrayList<>());
    }
    
    System.out.println("Emergency solution created for " + emergencyStops.size() + " trucks");
    return new Routes(emergencyStops, emergencyPaths, 999.0); // Alto costo para indicar que es subóptima
  }

  /**
   * TEMPORAL: Crea roadblocks simulados basados en patrones típicos de la red.
   * En producción, esto debería venir del OptimizerContext.
   */
  private List<Roadblock> createSimulatedRoadblocks() {
    List<Roadblock> roadblocks = new ArrayList<>();
    
    // Tiempo actual para simulación
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime futureTime = now.plusHours(24); // Roadblocks activos por 24 horas
    
    // Basado en la imagen mostrada por el usuario, simular roadblocks típicos
    // Crear roadblocks en intersecciones críticas
    
    // Roadblock 1: Centro de la red
    roadblocks.add(new Roadblock(now, futureTime, List.of(
        new Point(50, 50),
        new Point(51, 50),
        new Point(50, 51)
    )));
    
    // Roadblock 2: Zona norte-oeste
    roadblocks.add(new Roadblock(now, futureTime, List.of(
        new Point(25, 75),
        new Point(26, 75),
        new Point(25, 76)
    )));
    
    // Roadblock 3: Zona sur-este
    roadblocks.add(new Roadblock(now, futureTime, List.of(
        new Point(75, 25),
        new Point(76, 25),
        new Point(75, 26)
    )));
    
    // Roadblock 4: Zona centro-norte
    roadblocks.add(new Roadblock(now, futureTime, List.of(
        new Point(40, 60),
        new Point(41, 60),
        new Point(40, 61)
    )));
    
    // Roadblock 5: Zona centro-sur
    roadblocks.add(new Roadblock(now, futureTime, List.of(
        new Point(60, 40),
        new Point(61, 40),
        new Point(60, 41)
    )));
    
    System.out.println("✅ ROADBLOCKS SIMULADOS: Se crearon " + roadblocks.size() + " roadblocks simulados para activar el sistema de reubicación de órdenes");
    
    return roadblocks;
  }

}
