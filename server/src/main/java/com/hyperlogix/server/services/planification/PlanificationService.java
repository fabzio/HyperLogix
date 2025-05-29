package com.hyperlogix.server.services.planification;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.hyperlogix.server.domain.PLGNetwork;
import com.hyperlogix.server.domain.Routes;
import com.hyperlogix.server.optimizer.AntColony.AntColonyConfig;
import com.hyperlogix.server.optimizer.AntColony.AntColonyOptmizer;
import com.hyperlogix.server.optimizer.Optimizer;
import com.hyperlogix.server.optimizer.OptimizerContext;
import com.hyperlogix.server.optimizer.OptimizerResult;

@Service
public class PlanificationService {

  public Routes generateRoutes(PLGNetwork network) {
    AntColonyConfig config = new AntColonyConfig();
    Optimizer optimizer = new AntColonyOptmizer(config);

    OptimizerContext ctx = new OptimizerContext(
        network,
        LocalDateTime.now()
    );

    OptimizerResult result = optimizer.run(ctx, Duration.ofSeconds(5));
    return result.getRoutes();
  }
}