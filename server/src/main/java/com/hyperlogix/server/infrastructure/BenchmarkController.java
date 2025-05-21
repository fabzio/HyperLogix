package com.hyperlogix.server.infrastructure;

import com.hyperlogix.server.benchmark.BenchmarkService;
import com.hyperlogix.server.domain.PLGNetwork;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class BenchmarkController {
  private final BenchmarkService benchmarkService;

  public BenchmarkController(BenchmarkService benchmarkService) {
    this.benchmarkService = benchmarkService;
  }

  @PostMapping("/benchmark/start")
  public ResponseEntity<Void> startBenchmark() {

    new Thread(() -> benchmarkService.startBenchmark()).start();
    return ResponseEntity.ok().build();
  }
}
