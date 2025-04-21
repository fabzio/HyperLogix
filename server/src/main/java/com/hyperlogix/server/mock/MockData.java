package com.hyperlogix.server.mock;

import com.hyperlogix.server.domain.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

public class MockData {
        public static PLGNetwork mockNetwork() {
                List<Truck> trucks = List.of(
                                new Truck("T1", "TT01", TruckType.TA, TruckState.ACTIVE, 5.0, 10, 10, 50.0, 50.0,
                                                LocalDateTime.now().plusMonths(1), new Point(0, 0)),

                                new Truck("T2", "TT02", TruckType.TB, TruckState.ACTIVE, 6.0, 12, 12, 60.0, 60.0,
                                                LocalDateTime.now().plusMonths(2), new Point(1, 0)));

                List<Station> stations = List.of(
                                new Station("S1", "Station1", new Point(0, 0), 160, true, new HashMap<>()),
                                new Station("S2", "Station2", new Point(5, 5), (int) Double.POSITIVE_INFINITY, false,
                                                new HashMap<>()));

                List<Order> orders = List.of(
                                new Order("O1", "C1", LocalDateTime.now().plusHours(6), new Point(3, 3), 5, 0,
                                                Duration.ofHours(2)),
                                new Order("O2", "C2", LocalDateTime.now().plusHours(8), new Point(6, 6), 8, 0,
                                                Duration.ofHours(3)));

                List<Incident> incidents = List.of();

                List<Roadblock> roadblocks = List.of(
                                new Roadblock(LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(3),
                                                List.of(new Point(2, 2), new Point(3, 3))));

                return new PLGNetwork(trucks, stations, orders, incidents, roadblocks);
        }
}
