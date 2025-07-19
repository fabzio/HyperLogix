package com.hyperlogix.server.config;

import java.security.Principal;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .csrf(csrf -> csrf.disable())
        .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
    return http.build();
  }

  // 🧠 Interceptor que lee el username desde la URL y lo guarda
  @Bean
  public HandshakeInterceptor userInterceptor() {
    return new HandshakeInterceptor() {
      @Override
      public boolean beforeHandshake(
          @NonNull ServerHttpRequest request,
          @NonNull ServerHttpResponse response,
          @NonNull WebSocketHandler wsHandler,
          @NonNull Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {
          String query = servletRequest.getServletRequest().getQueryString();
          if (query != null && query.contains("user=")) {
            String username = query.split("user=")[1];
            attributes.put("username", username);
          }
        }
        return true;
      }

      @Override
      public void afterHandshake(
          @NonNull ServerHttpRequest request,
          @NonNull ServerHttpResponse response,
          @NonNull WebSocketHandler wsHandler,
          @Nullable Exception exception) {
      }
    };
  }

  @Bean
  public DefaultHandshakeHandler handshakeHandler() {
    return new DefaultHandshakeHandler() {
      @Override
      protected Principal determineUser(
          @NonNull ServerHttpRequest request,
          @NonNull WebSocketHandler wsHandler,
          @NonNull Map<String, Object> attributes) {

        String username = (String) attributes.get("username");
        return () -> username != null ? username : "anonymous";
      }
    };
  }
}

