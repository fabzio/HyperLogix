package com.hyperlogix.server.infrastructure;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.features.orders.repository.OrderRepository;
import com.hyperlogix.server.features.orders.utils.OrderMapper;
import com.hyperlogix.server.mock.MockData;

@Component
@Profile("!prod") // Solo en desarrollo
public class OrderDataLoader implements CommandLineRunner {
  @Autowired
  private OrderRepository orderRepository;

  @Override
  public void run(String... args) {
    if (orderRepository.findAll().isEmpty()) {
      String dataDir = "src/main/java/com/hyperlogix/server/benchmark/pedidos.20250419/";
      List<String> orderFilePaths = List.of(
          dataDir + "ventas202501.txt",
          dataDir + "ventas202502.txt",
          dataDir + "ventas202503.txt",
          dataDir + "ventas202505.txt",
          dataDir + "ventas202506.txt",
          dataDir + "ventas202507.txt",
          dataDir + "ventas202508.txt",
          dataDir + "ventas202509.txt",
          dataDir + "ventas202510.txt",
          dataDir + "ventas202511.txt",
          dataDir + "ventas202512.txt",
          dataDir + "ventas202601.txt",
          dataDir + "ventas202603.txt",
          dataDir + "ventas202604.txt",
          dataDir + "ventas202605.txt",
          dataDir + "ventas202606.txt",
          dataDir + "ventas202607.txt",
          dataDir + "ventas202608.txt",
          dataDir + "ventas202610.txt",
          dataDir + "ventas202611.txt",
          dataDir + "ventas202612.txt");
      List<Order> orders = MockData.loadOrdersFromFiles(orderFilePaths,-1);

      orderRepository.saveAll(orders.stream().map(OrderMapper::mapToEntity).toList());
    }
  }
}
