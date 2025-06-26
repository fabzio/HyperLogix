package com.hyperlogix.server.features.operation.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.hyperlogix.server.features.operation.services.RealTimeOperationService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RealTimeOperationConfig {

  private final RealTimeOperationService realTimeOperationService;

  public RealTimeOperationConfig(RealTimeOperationService realTimeOperationService) {
    this.realTimeOperationService = realTimeOperationService;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    log.info("Application is ready, initializing real-time operation system...");
    // The @PostConstruct in RealTimeOperationService will handle initialization
  }
}
