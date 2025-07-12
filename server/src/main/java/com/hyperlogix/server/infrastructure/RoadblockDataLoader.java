package com.hyperlogix.server.infrastructure;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hyperlogix.server.domain.Roadblock;
import com.hyperlogix.server.features.blocks.repository.RoadblockRepository;
import com.hyperlogix.server.features.blocks.utils.BlockMapper;
import com.hyperlogix.server.mock.MockData;

import lombok.extern.slf4j.Slf4j;

@Component
@Profile("!prod")
@Slf4j
public class RoadblockDataLoader implements CommandLineRunner {

  // Fixed batch size for processing
  private static final int BATCH_SIZE = 500;

  @Autowired
  private RoadblockRepository roadblockRepository;

  @Override
  public void run(String... args) {
    if (roadblockRepository.findAll().isEmpty()) {
      String dataDir = "src/main/java/com/hyperlogix/server/mock/bloqueos.20250419/";
      List<String> roadblocksFilePaths = List.of(
          dataDir + "202501.bloqueos.txt",
          dataDir + "202502.bloqueos.txt",
          dataDir + "202503.bloqueos.txt",
          dataDir + "202504.bloqueos.txt",
          dataDir + "202505.bloqueos.txt",
          dataDir + "202506.bloqueos.txt",
          dataDir + "202507.bloqueos.txt",
          dataDir + "202508.bloqueos.txt",
          dataDir + "202509.bloqueos.txt",
          dataDir + "202510.bloqueos.txt",
          dataDir + "202511.bloqueos.txt",
          dataDir + "202512.bloqueos.txt");

      List<Roadblock> roadblocks = MockData.loadRoadlocksFromFiles(roadblocksFilePaths, -1);
      log.info("Loaded {} roadblocks from files, starting batch save process...",
          roadblocks.size());

      saveBatches(roadblocks);

    } else {
    }
  }

  @Transactional
  private void saveBatches(List<Roadblock> roadblocks) {
    int totalRoadblocks = roadblocks.size();
    int totalBatches = (int) Math.ceil((double) totalRoadblocks / BATCH_SIZE);

    for (int i = 0; i < totalRoadblocks; i += BATCH_SIZE) {
      int endIndex = Math.min(i + BATCH_SIZE, totalRoadblocks);
      List<Roadblock> batch = roadblocks.subList(i, endIndex);

      int currentBatch = (i / BATCH_SIZE) + 1;
      log.info("Processing batch {}/{} ({} roadblocks)...", currentBatch,
          totalBatches, batch.size());

      try {
        List<com.hyperlogix.server.features.blocks.entity.BlockEntity> entities = batch.stream()
            .map(BlockMapper::mapToEntity).toList();

        roadblockRepository.saveAll(entities);
      } catch (Exception e) {
        log.error("Failed to save batch {}/{}: {}", currentBatch, totalBatches,
            e.getMessage());
        throw new RuntimeException("Batch save failed", e);
      }
    }
  }
}