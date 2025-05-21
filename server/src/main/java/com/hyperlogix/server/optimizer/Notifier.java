package com.hyperlogix.server.optimizer;

@FunctionalInterface
public interface Notifier {
  void notify(OptimizerResult message);
}
