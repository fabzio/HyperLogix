package com.hyperlogix.server.features.simulation.dtos;

public class SimulationCommandRequest extends SimulationRequest {
  private String command;

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }
}
