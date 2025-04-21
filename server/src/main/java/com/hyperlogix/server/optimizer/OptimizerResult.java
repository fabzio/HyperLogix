package com.hyperlogix.server.optimizer;

import com.hyperlogix.server.domain.Routes;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OptimizerResult {
  private Routes routes;
  private double cost;
}
