package com.hyperlogix.server.services.planification;

import com.hyperlogix.server.features.planification.dtos.PlanificationResultNotification;

@FunctionalInterface
public interface PlanificationNotifier {
  void notify(PlanificationResultNotification planificationResultNotification);
}
