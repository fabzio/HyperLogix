package com.hyperlogix.server.config;

import java.time.Duration;
import java.time.LocalTime;
import java.time.Period;

public class Constants {
  /**
   * Peso del GLP en toneladas
   */
  public static double GLP_WEIGHT = 0.5;
  /**
   * Distancia entre nodos en km
   */
  public static int EDGE_LENGTH = 1;
  /**
   * Periodo de mantenimiento de los camiones
   */
  public static Period MAINTENANCE_TRUCK_PERIOD = Period.ofMonths(2);
  /**
   * Velocidad promedio de los camiones en km/h
   */
  public static double TRUCK_SPEED = 50.0;
  /**
   *
   */
  public static Duration MIN_DELIVERY_TIME = Duration.ofHours(4);
  /**
   *
   */
  public static LocalTime WAREHOUSE_RESTOCK_TIME = LocalTime.of(0, 0);

  public static int MAP_WIDTH = 70;
  public static int MAP_HEIGHT = 50;
}
