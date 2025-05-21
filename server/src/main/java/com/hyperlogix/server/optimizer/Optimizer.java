package com.hyperlogix.server.optimizer;

import java.time.Duration;

public interface Optimizer {
  public OptimizerResult run(OptimizerContext ctx, Duration maxDuration, Notifier notifier);

  public default OptimizerResult run(OptimizerContext ctx, Duration maxDuration) {
    return run(ctx, maxDuration, null); // Pass null for no notifications by default
  }
}
