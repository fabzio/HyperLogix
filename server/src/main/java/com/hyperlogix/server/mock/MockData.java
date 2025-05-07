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
  private static final Pattern ORDER_LINE_PATTERN = Pattern.compile(
      "(\\d+)d(\\d+)h(\\d+)m:(\\d+),(\\d+),c-(\\d+),(\\d+)m3,(\\d+)h");

  public static List<Order> loadOrdersFromFiles(List<String> filePaths, int offset, int limit) {
    List<Order> orders = new ArrayList<>();
    if (filePaths == null) {
      return orders;
    }

    int successfullyParsedOrdersCount = 0; // Counts all valid orders encountered to apply offset
    int ordersAddedCount = 0; // Counts orders actually added to the list to apply limit

    for (String filePath : filePaths) {
      // If a positive limit is set and reached, stop processing more files.
      if (limit > 0 && ordersAddedCount >= limit) {
        break;
      }

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
          while ((line = reader.readLine()) != null) {
            // If a positive limit is set and reached, stop processing more lines in the
            // current file.
            if (limit > 0 && ordersAddedCount >= limit) {
              break;
            }

            Matcher orderLineMatcher = ORDER_LINE_PATTERN.matcher(line);
            if (orderLineMatcher.matches()) {
              successfullyParsedOrdersCount++;

              // Skip orders if current count is less than or equal to offset
              if (successfullyParsedOrdersCount <= offset) {
                continue;
              }

              int day = Integer.parseInt(orderLineMatcher.group(1));
              int hour = Integer.parseInt(orderLineMatcher.group(2));
              int minute = Integer.parseInt(orderLineMatcher.group(3));
              int posX = Integer.parseInt(orderLineMatcher.group(4));
              int posY = Integer.parseInt(orderLineMatcher.group(5));
              String clientId = "c-" + orderLineMatcher.group(6);
              int quantity = Integer.parseInt(orderLineMatcher.group(7));
              long limitHours = Long.parseLong(orderLineMatcher.group(8));

              LocalDateTime arrivalTime = LocalDateTime.of(year, Month.of(month), day, hour, minute);
              Point location = new Point(posX, posY);
              String orderId = UUID.randomUUID().toString();

              orders.add(new Order(orderId, clientId, arrivalTime, location, quantity, 0,
                  Duration.ofHours(limitHours)));
              ordersAddedCount++;
            } else {
              System.err.println("Skipping malformed line in " + filePath + ": " + line);
            }
          }
        }
      } catch (IOException e) {
        System.err.println("Error reading file " + filePath + ": " + e.getMessage());
      } catch (NumberFormatException e) {
        System.err.println("Error parsing number in file " + filePath + ": " + e.getMessage());
      } catch (Exception e) {
        System.err.println("An unexpected error occurred while processing file " + filePath + ": " + e.getMessage());
      }
    }
    return orders;
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
        new Station("S1", "Central", new Point(12, 8), Integer.MAX_VALUE, true, new HashMap<>()), // Adjusted
        new Station("S2", "Intermedio Norte", new Point(42, 42), 160, false, new HashMap<>()), // Adjusted
        new Station("S3", "Intermedio Este", new Point(63, 3), 160, false,
            new HashMap<>()));

    List<Order> orders = List.of();
    List<Incident> incidents = List.of();
    List<Roadblock> roadblocks = List.of();

    return new PLGNetwork(trucks, stations, orders, incidents, roadblocks);
  }

  public static PLGNetwork mockNetworkWithOrdersFromFiles(List<String> orderFilePaths) {
    // Consider if this method also needs offset/limit, or if it should always load
    // all.
    // For now, it will load all by passing default offset 0 and limit -1.
    List<Order> orders = loadOrdersFromFiles(orderFilePaths, 0, -1);

    // Populate other network components (trucks, stations, etc.) as needed
    List<Truck> trucks = new ArrayList<>(); // Add mock trucks
    List<Station> stations = new ArrayList<>(); // Add mock stations
    List<Incident> incidents = Collections.emptyList();
    List<Roadblock> roadblocks = Collections.emptyList();

    return new PLGNetwork(trucks, stations, orders, incidents, roadblocks);
  }
}
