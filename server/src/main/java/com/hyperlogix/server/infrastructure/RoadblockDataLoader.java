package com.hyperlogix.server.infrastructure;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.hyperlogix.server.domain.Roadblock;
import com.hyperlogix.server.features.blocks.repository.RoadblockRepository;
import com.hyperlogix.server.features.blocks.utils.BlockMapper;
import com.hyperlogix.server.mock.MockData;

@Component
@Profile("!prod") 
public class RoadblockDataLoader implements CommandLineRunner {
  @Autowired
  private RoadblockRepository roadblockRepository;

  @Override
  public void run(String... args) {
    if (roadblockRepository.findAll().isEmpty()) {
      String dataDir = "src/main/java/com/hyperlogix/server/benchmark/bloqueos.20250419/";
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
      List<Roadblock> roadblocks = MockData.loadRoadlocksFromFiles(roadblocksFilePaths,-1);

      roadblockRepository.saveAll(roadblocks.stream().map(BlockMapper::mapToEntity).toList());
    }
  }
}