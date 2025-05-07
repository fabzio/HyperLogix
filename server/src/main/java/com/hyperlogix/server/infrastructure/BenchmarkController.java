package com.hyperlogix.server.infrastructure;

import com.hyperlogix.server.benchmark.BenchmarkService;
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
    benchmarkService.startBenchmark();
    return ResponseEntity.ok().build();
  }
}
