package com.hyperlogix.server.mock;

import com.hyperlogix.server.domain.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MockData {

  private static final Pattern FILENAME_PATTERN = Pattern.compile("ventas(\\d{4})(\\d{2})\\.txt");
  private static final Pattern FILENAME_PATTERN_BLOCKS = Pattern.compile("(\\d{4})(\\d{2})\\.bloqueos\\.txt");
  private static final Pattern ORDER_LINE_PATTERN = Pattern.compile(
      "(\\d+)d(\\d+)h(\\d+)m:(\\d+),(\\d+),c-(\\d+),(\\d+)m3,(\\d+)h");
  private static final Pattern ORDER_LINE_PATTERN_BLOCKS = Pattern.compile(
    "^(\\d{2}d\\d{2}h\\d{2}m)-(\\d{2}d\\d{2}h\\d{2}m):((\\d{2},\\d{2})(,\\d{2},\\d{2})*)$"
);

  public static List<Order> loadOrdersFromFiles(List<String> filePaths, int limit) {
    List<Order> orders = new ArrayList<>();
    if (filePaths == null) {
      return orders;
    }

    int orderCount = 0;
    boolean loadAll = limit < 0;

    for (String filePath : filePaths) {
      try {
        String fileName = Paths.get(filePath).getFileName().toString();
        Matcher fileNameMatcher = FILENAME_PATTERN.matcher(fileName);
        if (!fileNameMatcher.matches()) {
          System.err.println("Skipping file with invalid name format: " + filePath);
          continue;
        }

        int year = Integer.parseInt(fileNameMatcher.group(1));
        int month = Integer.parseInt(fileNameMatcher.group(2));

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
          String line;
          while ((line = reader.readLine()) != null && (loadAll || orderCount < limit)) {
            Matcher orderLineMatcher = ORDER_LINE_PATTERN.matcher(line);
            if (orderLineMatcher.matches()) {
              int day = Integer.parseInt(orderLineMatcher.group(1));
              int hour = Integer.parseInt(orderLineMatcher.group(2));
              int minute = Integer.parseInt(orderLineMatcher.group(3));
              LocalDateTime arrivalTime = LocalDateTime.of(year, Month.of(month), day, hour, minute);

              orders.add(new Order(
                  UUID.randomUUID().toString(),
                  "c-" + orderLineMatcher.group(6),
                  arrivalTime,
                  new Point(Integer.parseInt(orderLineMatcher.group(4)), Integer.parseInt(orderLineMatcher.group(5))),
                  Integer.parseInt(orderLineMatcher.group(7)),
                  0,
                  Duration.ofHours(Long.parseLong(orderLineMatcher.group(8))),
                  OrderStatus.PENDING));

              orderCount++;
            } else {
              System.err.println("Skipping malformed line in " + filePath + ": " + line);
            }
          }
        }
      } catch (IOException | NumberFormatException e) {
        System.err.println("Error processing file " + filePath + ": " + e.getMessage());
      }
    }
    return orders;
  }

    public static List<Roadblock> loadRoadlocksFromFiles(List<String> filePaths, int limit) {
    List<Roadblock> roadblocks = new ArrayList<>();
    if (filePaths == null) {
      return roadblocks;
    }

    int orderCount = 0;
    boolean loadAll = limit < 0;

    for (String filePath : filePaths) {
      try {
        String fileName = Paths.get(filePath).getFileName().toString();
        Matcher fileNameMatcher = FILENAME_PATTERN_BLOCKS.matcher(fileName);
        if (!fileNameMatcher.matches()) {
          System.err.println("Skipping file with invalid name format: " + filePath);
          continue;
        }

        int year = Integer.parseInt(fileNameMatcher.group(1));
        int month = Integer.parseInt(fileNameMatcher.group(2));

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
          String line;
          while ((line = reader.readLine()) != null && (loadAll || orderCount < limit)) {
            Matcher orderLineMatcher = ORDER_LINE_PATTERN_BLOCKS.matcher(line);
            if (orderLineMatcher.matches()) {              
              String startTimeStr = orderLineMatcher.group(1);
              int startDay = Integer.parseInt(startTimeStr.substring(0, 2));
              int startHour = Integer.parseInt(startTimeStr.substring(3, 5));
              int startMinute = Integer.parseInt(startTimeStr.substring(6, 8));

              String endTimeStr = orderLineMatcher.group(2);
              int endDay = Integer.parseInt(endTimeStr.substring(0, 2));
              int endHour = Integer.parseInt(endTimeStr.substring(3, 5));
              int endMinute = Integer.parseInt(endTimeStr.substring(6, 8));

              LocalDateTime startTime = LocalDateTime.of(year, Month.of(month), startDay, startHour, startMinute);
              LocalDateTime endTime = LocalDateTime.of(year, Month.of(month), endDay, endHour, endMinute);

              String[] coordinates = orderLineMatcher.group(3).split(",");
              List<Point> points = new ArrayList<>();
              for (int i = 0; i < coordinates.length; i += 2) {
                  points.add(new Point(
                      Integer.parseInt(coordinates[i]),
                      Integer.parseInt(coordinates[i + 1])
                  ));
              }

              roadblocks.add(new Roadblock(
                  startTime,
                  endTime,
                  points));
            } else {
              System.err.println("Skipping malformed line in " + filePath + ": " + line);
            }
          }
        }
      } catch (IOException | NumberFormatException e) {
        System.err.println("Error processing file " + filePath + ": " + e.getMessage());
      }
    }
    return roadblocks;
  }

  public static PLGNetwork mockNetwork() {
    List<Truck> trucks = List.of(
        new Truck("T1", "TA01", TruckType.TA, TruckState.ACTIVE, 2.5,
            25,
            25,
            25,
            25,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T2", "TA02", TruckType.TA, TruckState.ACTIVE, 2.5,
            25,
            25,
            25,
            25,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T3", "TB01", TruckType.TB, TruckState.ACTIVE, 2.0,
            15,
            15,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T4", "TB02", TruckType.TB, TruckState.ACTIVE, 2.0,
            15,
            15,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T5", "TB03", TruckType.TB, TruckState.ACTIVE, 2.0,
            15,
            15,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T6", "TB04", TruckType.TB, TruckState.ACTIVE, 2.0,
            15,
            15,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T7", "TC01", TruckType.TC, TruckState.ACTIVE, 1.5,
            10,
            10,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T8", "TC02", TruckType.TC, TruckState.ACTIVE, 1.5,
            10,
            10,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T9", "TC03", TruckType.TC, TruckState.ACTIVE, 1.5,
            10,
            10,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T10", "TC04", TruckType.TC, TruckState.ACTIVE, 1.5,
            10,
            10,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T11", "TD01", TruckType.TD, TruckState.ACTIVE, 1.0,
            5,
            5,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T12", "TD02", TruckType.TD, TruckState.ACTIVE, 1.0,
            5,
            5,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T13", "TD03", TruckType.TD, TruckState.ACTIVE, 1.0,
            5,
            5,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T14", "TD04", TruckType.TD, TruckState.ACTIVE, 1.0,
            5,
            5,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T15", "TD05", TruckType.TD, TruckState.ACTIVE, 1.0,
            5,
            5,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T16", "TD06", TruckType.TD, TruckState.ACTIVE, 1.0,
            5,
            5,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T17", "TD07", TruckType.TD, TruckState.ACTIVE, 1.0,
            5,
            5,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T18", "TD08", TruckType.TD, TruckState.ACTIVE, 1.0,
            5,
            5,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T19", "TD09", TruckType.TD, TruckState.ACTIVE, 1.0,
            5,
            5,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)),
        new Truck("T20", "TD010", TruckType.TD, TruckState.ACTIVE, 1.0,
            5,
            5,
            25.0,
            25.0,
            LocalDateTime.now().plusMonths(6),
            new Point(12, 8)));

    List<Station> stations = List.of(
        new Station("S1", "Central", new Point(12, 8), Integer.MAX_VALUE, true, new HashMap<>(), new ArrayList<>()), // Adjusted
        new Station("S2", "Intermedio Norte", new Point(42, 42), 160, false, new HashMap<>(), new ArrayList<>()), // Adjusted
        new Station("S3", "Intermedio Este", new Point(63, 3), 160, false, new HashMap<>(), new ArrayList<>()));

    List<Order> orders = List.of();
    List<Incident> incidents = List.of();
    List<Roadblock> roadblocks = List.of();

    return new PLGNetwork(trucks, stations, orders, incidents, roadblocks);
  }

  public static PLGNetwork mockNetworkWithOrdersFromFiles(List<String> orderFilePaths) {
    // Consider if this method also needs offset/limit, or if it should always load
    // all.
    // For now, it will load all by passing default offset 0 and limit -1.
    int limit = 50; // Example limit
    List<Order> orders = loadOrdersFromFiles(orderFilePaths, limit);

    // Populate other network components (trucks, stations, etc.) as needed
    List<Truck> trucks = new ArrayList<>(); // Add mock trucks
    List<Station> stations = new ArrayList<>(); // Add mock stations
    List<Incident> incidents = Collections.emptyList();
    List<Roadblock> roadblocks = Collections.emptyList();

    return new PLGNetwork(trucks, stations, orders, incidents, roadblocks);
  }
}

