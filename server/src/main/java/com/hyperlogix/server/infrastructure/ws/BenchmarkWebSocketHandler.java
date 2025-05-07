package com.hyperlogix.server.infrastructure.ws;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class BenchmarkWebSocketHandler {

  /**
   * Handles messages sent to the "/app/stomp-test" destination.
   * Received messages will be processed, and a response will be sent to
   * "/topic/stomp-response".
   *
   * @param message The message received from the client.
   * @return The response message to be sent to subscribed clients.
   */
  @MessageMapping("/benchmark") // Clients send messages to /app/benchmark
  @SendTo("/topic/benchmark") // Responses are sent to /topic/benchmark
  public String handleStompTestMessage(String message) {
    System.out.println("Received STOMP message: " + message);
    // Process the message and return a response
    return "Server received: " + message + ". Responding via STOMP.";
  }
}
