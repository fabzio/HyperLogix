package com.hyperlogix.server.infrastructure;

import com.hyperlogix.server.features.trucks.repository.TruckRepository;
import com.hyperlogix.server.features.trucks.utils.TruckMapper;
import com.hyperlogix.server.domain.Point;
import com.hyperlogix.server.domain.Truck;
import com.hyperlogix.server.domain.TruckType;
import com.hyperlogix.server.domain.TruckState;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("!prod") // Solo en desarrollo
public class TruckDataLoader implements CommandLineRunner, org.springframework.core.Ordered {
  
  // Set execution order to run before IncidentDataLoader (lower number = higher priority)
  private static final int EXECUTION_ORDER = 50;
  
  @Autowired
  private TruckRepository truckRepository;

  /**
   * Set execution order to run before IncidentDataLoader
   */
  @Override
  public int getOrder() {
    return EXECUTION_ORDER;
  }

  @Override
  public void run(String... args) {
    if (truckRepository.findAll().isEmpty()) {
      List<Truck> trucks = new ArrayList<>();
      // TA: 2 camiones de 25m3
      for (int i = 1; i <= 2; i++) {
        Truck t = new Truck();
        t.setCode("TA" + String.format("%02d", i));
        t.setType(TruckType.TA);
        t.setStatus(TruckState.IDLE);
        t.setTareWeight(2.5);
        t.setMaxCapacity(25);
        t.setCurrentCapacity(25);
        t.setFuelCapacity(25);
        t.setCurrentFuel(25);
        t.setLocation(new Point(12, 8));
        trucks.add(t);
      }
      // TB: 4 camiones de 15m3
      for (int i = 1; i <= 4; i++) {
        Truck t = new Truck();
        t.setCode("TB" + String.format("%02d", i));
        t.setType(TruckType.TB);
        t.setStatus(TruckState.IDLE);
        t.setTareWeight(2.0);
        t.setMaxCapacity(15);
        t.setCurrentCapacity(15);
        t.setFuelCapacity(25);
        t.setCurrentFuel(25);
        t.setLocation(new Point(12, 8));
        trucks.add(t);
      }
      // TC: 4 camiones de 10m3
      for (int i = 1; i <= 4; i++) {
        Truck t = new Truck();
        t.setCode("TC" + String.format("%02d", i));
        t.setType(TruckType.TC);
        t.setStatus(TruckState.IDLE);
        t.setTareWeight(1.5);
        t.setMaxCapacity(10);
        t.setCurrentCapacity(10);
        t.setFuelCapacity(25);
        t.setCurrentFuel(25);
        t.setLocation(new Point(12, 8));
        trucks.add(t);
      }
      // TD: 10 camiones de 5m3
      for (int i = 1; i <= 10; i++) {
        Truck t = new Truck();
        t.setCode("TD" + String.format("%02d", i));
        t.setType(TruckType.TD);
        t.setStatus(TruckState.IDLE);
        t.setTareWeight(1.0);
        t.setMaxCapacity(5);
        t.setCurrentCapacity(5);
        t.setFuelCapacity(25);
        t.setCurrentFuel(25);
        t.setLocation(new Point(12, 8));
        trucks.add(t);
      }

      truckRepository.saveAll(trucks.stream().map(TruckMapper::mapToEntity).toList());
    }
  }
}
