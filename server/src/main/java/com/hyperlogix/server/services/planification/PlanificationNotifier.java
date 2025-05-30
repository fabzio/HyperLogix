package com.hyperlogix.server.services.planification;

import com.hyperlogix.server.domain.Routes;

@FunctionalInterface
public interface PlanificationNotifier {
  void notify(Routes routes);
}
