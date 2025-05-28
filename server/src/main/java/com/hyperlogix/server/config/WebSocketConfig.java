package com.hyperlogix.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  @Autowired
  private DefaultHandshakeHandler handshakeHandler;

  @Autowired
  private HandshakeInterceptor userInterceptor;

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
}
