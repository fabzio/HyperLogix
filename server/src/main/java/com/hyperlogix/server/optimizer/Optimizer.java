package com.hyperlogix.server.optimizer;

import java.time.Duration;

public interface Optimizer {
  public OptimizerResult run(OptimizerContext ctx, Duration maxDuration, boolean verbose);

  public default OptimizerResult run(OptimizerContext ctx, Duration maxDuration) {
    return run(ctx, maxDuration, false);
  }
}
