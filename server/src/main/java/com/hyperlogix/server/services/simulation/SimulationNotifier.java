package com.hyperlogix.server.services.simulation;

@FunctionalInterface
public interface SimulationNotifier {
  void notifySnapshot(SimulationSnapshot snapshot);
}
