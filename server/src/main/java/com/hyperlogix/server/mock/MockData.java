package com.hyperlogix.server.mock;

import com.hyperlogix.server.domain.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList; // Import ArrayList
import java.util.Arrays; // Import Arrays
import java.util.HashMap;
import java.util.List;

public class MockData {
        public static PLGNetwork mockNetwork() {
                // More realistic truck data with updated positive coordinates
                List<Truck> trucks = List.of(
                                // Truck starting at the main station
                                new Truck("T1", "TT01", TruckType.TA, TruckState.ACTIVE, 5.0, // speed
                                                100, // capacity GLP
                                                100, // current GLP
                                                80.0, // fuel capacity
                                                80.0, // current fuel
                                                LocalDateTime.now().plusMonths(6), // maintenance date
                                                new Point(5, 5)), // Start at main station S1 (adjusted)

                                // Truck starting elsewhere, different type
                                new Truck("T2", "TT02", TruckType.TB, TruckState.ACTIVE, 4.5, // speed
                                                120, // capacity GLP
                                                110, // current GLP
                                                90.0, // fuel capacity
                                                70.0, // current fuel
                                                LocalDateTime.now().plusMonths(4), // maintenance date
                                                new Point(10, 15)), // Start location (adjusted)

                                // Third truck, potentially lower fuel or capacity
                                new Truck("T3", "TT03", TruckType.TA, TruckState.ACTIVE, 5.5, // speed
                                                90, // capacity GLP
                                                90, // current GLP
                                                70.0, // fuel capacity
                                                65.0, // current fuel
                                                LocalDateTime.now().plusMonths(8), // maintenance date
                                                new Point(20, 8)) // Start location (adjusted)
                );

                // Stations with varied locations and capacities (adjusted positive coordinates)
                List<Station> stations = List.of(
                                // Main station with large capacity
                                new Station("S1", "Main Station Alpha", new Point(5, 5), 10000, true, new HashMap<>()), // Adjusted
                                // Secondary station
                                new Station("S2", "Refuel Point Bravo", new Point(35, 20), 160, false, new HashMap<>()), // Adjusted
                                // Another secondary station
                                new Station("S3", "Supply Hub Charlie", new Point(50, 10), 160, false,
                                                new HashMap<>())); // Adjusted

                // Orders with varied locations, demands, and time windows (adjusted positive
                // coordinates)
                List<Order> orders = new ArrayList<>(Arrays.asList(
                                new Order("O1", "Client A", LocalDateTime.now().plusHours(8), // Deadline
                                                new Point(28, 42), // Location (adjusted)
                                                50, // Requested GLP
                                                0, // Delivered GLP
                                                Duration.ofHours(1)), // Service time at location

                                new Order("O2", "Client B", LocalDateTime.now().plusHours(10), // Deadline
                                                new Point(15, 8), // Location (adjusted)
                                                70, // Requested GLP
                                                0, // Delivered GLP
                                                Duration.ofMinutes(90)), // Service time

                                new Order("O3", "Client C", LocalDateTime.now().plusHours(12), // Deadline
                                                new Point(60, 15), // Location (adjusted)
                                                30, // Requested GLP
                                                0, // Delivered GLP
                                                Duration.ofMinutes(45)), // Service time

                                new Order("O4", "Client D", LocalDateTime.now().plusHours(9), // Deadline
                                                new Point(45, 35), // Location (adjusted)
                                                60, // Requested GLP
                                                0, // Delivered GLP
                                                Duration.ofHours(1)), // Service time

                                // Additional Orders (adjusted positive coordinates)
                                new Order("O5", "Client E", LocalDateTime.now().plusHours(11), // Deadline
                                                new Point(10, 45), // Location (adjusted)
                                                45, // Requested GLP
                                                0, // Delivered GLP
                                                Duration.ofMinutes(60)), // Service time

                                new Order("O6", "Client F", LocalDateTime.now().plusHours(14), // Deadline
                                                new Point(68, 20), // Location (adjusted)
                                                80, // Requested GLP
                                                0, // Delivered GLP
                                                Duration.ofMinutes(75)), // Service time

                                new Order("O7", "Client G", LocalDateTime.now().plusHours(7), // Deadline (earlier)
                                                new Point(12, 12), // Location (adjusted)
                                                25, // Requested GLP
                                                0, // Delivered GLP
                                                Duration.ofMinutes(30)) // Service time
                ));

                // Incidents (optional, keep empty or add one)
                List<Incident> incidents = List.of(
                // Example: new Incident("INC01", "Minor Delay Zone", new Point(7, 7),
                // LocalDateTime.now().plusHours(2), Duration.ofHours(1), 0.2) // 20% speed
                // reduction
                );

                // Roadblocks affecting potential routes (adjusted positive coordinates)
                List<Roadblock> roadblocks = List.of(
                                new Roadblock(LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(5), // Active
                                                                                                                  // time
                                                List.of(new Point(25, 25), new Point(40, 40))), // Segment blocked
                                                                                                // (adjusted)

                                // Additional Roadblocks (adjusted positive coordinates)
                                new Roadblock(LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(3), // Active
                                                                                                                  // time
                                                List.of(new Point(8, 8), new Point(15, 8))), // Segment blocked
                                                                                             // (adjusted)

                                new Roadblock(LocalDateTime.now().plusHours(6), LocalDateTime.now().plusHours(9), // Active
                                                                                                                  // time
                                                List.of(new Point(55, 5), new Point(60, 15))) // Segment blocked
                                                                                              // (adjusted)
                );

                return new PLGNetwork(trucks, stations, orders, incidents, roadblocks);
        }
}
