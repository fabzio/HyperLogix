package com.hyperlogix.server.infrastructure;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hyperlogix.server.domain.Order;
import com.hyperlogix.server.features.orders.repository.OrderRepository;
import com.hyperlogix.server.features.orders.utils.OrderMapper;
import com.hyperlogix.server.mock.MockData;

import lombok.extern.slf4j.Slf4j;

@Component
@Profile("!prod") // Solo en desarrollo
@Slf4j
public class OrderDataLoader implements CommandLineRunner {

  // Fixed batch size for processing
  private static final int BATCH_SIZE = 10000;

  @Autowired
  private OrderRepository orderRepository;

  @Override
  public void run(String... args) {
    if (orderRepository.findAll().isEmpty()) {
      log.info("Starting order data loading process...");

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

      List<Order> orders = MockData.loadOrdersFromFiles(orderFilePaths, -1);
      log.info("Loaded {} orders from files, starting batch save process...", orders.size());

      saveBatches(orders);

      log.info("Order data loading completed successfully!");
    } else {
      log.info("Orders already exist in database, skipping data loading.");
    }
  }
  @Transactional
  private void saveBatches(List<Order> orders) {
    int totalOrders = orders.size();
    int totalBatches = (int) Math.ceil((double) totalOrders / BATCH_SIZE);

    for (int i = 0; i < totalOrders; i += BATCH_SIZE) {
      int endIndex = Math.min(i + BATCH_SIZE, totalOrders);
      List<Order> batch = orders.subList(i, endIndex);

      int currentBatch = (i / BATCH_SIZE) + 1;
      log.info("Processing batch {}/{} ({} orders)...", currentBatch, totalBatches, batch.size());

      try {
        List<com.hyperlogix.server.features.orders.entity.OrderEntity> entities = batch.stream()
            .map(OrderMapper::mapToEntity).toList();

        orderRepository.saveAll(entities);
        log.info("Successfully saved batch {}/{}", currentBatch, totalBatches);

      } catch (Exception e) {
        log.error("Failed to save batch {}/{}: {}", currentBatch, totalBatches, e.getMessage());
        throw new RuntimeException("Batch save failed", e);
      }
    }
  }
}
