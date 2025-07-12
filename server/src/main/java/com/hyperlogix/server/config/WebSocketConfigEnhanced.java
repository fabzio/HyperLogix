package com.hyperlogix.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.lang.NonNull;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfigEnhanced implements WebSocketMessageBrokerConfigurer {
  
  @Autowired
  private DefaultHandshakeHandler handshakeHandler;

  @Autowired
  private HandshakeInterceptor userInterceptor;

  // Track active sessions for cleanup
  private final ConcurrentMap<String, Long> activeSessions = new ConcurrentHashMap<>();
  private final ScheduledExecutorService cleanupService = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "WebSocket-Cleanup");
    t.setDaemon(true);
    return t;
  });

  public WebSocketConfigEnhanced() {
    // Schedule cleanup every 30 seconds
    cleanupService.scheduleWithFixedDelay(this::cleanupStaleSessions, 30, 30, TimeUnit.SECONDS);
  }

  @Override
  public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic"); // Enables a simple in-memory broker for topics prefixed with /topic
    config.setApplicationDestinationPrefixes("/app"); // Sets the prefix for messages bound for @MessageMapping
                                                      // annotated methods
  }

  @Override
  public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .addInterceptors(userInterceptor)
        .setHandshakeHandler(handshakeHandler)
        .setAllowedOriginPatterns("*");
  }

  @Override
  public void configureWebSocketTransport(@NonNull WebSocketTransportRegistration registration) {
    registration.setMessageSizeLimit(2 * 1024 * 1024); // 2MB
    registration.setSendBufferSizeLimit(2 * 1024 * 1024); // 2MB
    registration.setSendTimeLimit(20000); // 20 seconds
    registration.setTimeToFirstMessage(30000); // 30 seconds timeout for first message
  }

  @EventListener
  public void handleSessionConnect(SessionConnectEvent event) {
    String sessionId = event.getMessage().getHeaders().get("simpSessionId").toString();
    activeSessions.put(sessionId, System.currentTimeMillis());
    log.debug("WebSocket session connected: {}", sessionId);
  }

  @EventListener
  public void handleSessionDisconnect(SessionDisconnectEvent event) {
    String sessionId = event.getSessionId();
    activeSessions.remove(sessionId);
    log.debug("WebSocket session disconnected: {}", sessionId);
  }

  private void cleanupStaleSessions() {
    long currentTime = System.currentTimeMillis();
    long staleThreshold = 5 * 60 * 1000; // 5 minutes
    
    activeSessions.entrySet().removeIf(entry -> {
      boolean isStale = (currentTime - entry.getValue()) > staleThreshold;
      if (isStale) {
        log.debug("Removing stale WebSocket session: {}", entry.getKey());
      }
      return isStale;
    });
  }

  @PreDestroy
  public void cleanup() {
    log.info("Cleaning up WebSocket resources...");
    cleanupService.shutdown();
    try {
      if (!cleanupService.awaitTermination(5, TimeUnit.SECONDS)) {
        cleanupService.shutdownNow();
      }
    } catch (InterruptedException e) {
      cleanupService.shutdownNow();
      Thread.currentThread().interrupt();
    }
    activeSessions.clear();
    log.info("WebSocket cleanup completed");
  }
}
