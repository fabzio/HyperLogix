package com.hyperlogix.server.simulation;

public interface SimulationNotifier {
  void notifySnapshot(SimulationSnapshot snapshot);
}
