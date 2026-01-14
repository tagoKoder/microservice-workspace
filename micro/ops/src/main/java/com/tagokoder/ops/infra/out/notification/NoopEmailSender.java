package com.tagokoder.ops.infra.out.notification;

import com.tagokoder.ops.domain.model.Channel;
import com.tagokoder.ops.domain.port.out.NotificationSenderPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.util.Map;

public class NoopEmailSender implements NotificationSenderPort {

  @Override public Channel channel() { return Channel.EMAIL; }

  @Override
  @CircuitBreaker(name = "emailProvider", fallbackMethod = "fallback")
  public void send(String topic, Map<String, Object> payload) {
    // NO loggear payload completo (ASVS V8/V10). Solo metadata.
    System.out.println("[EMAIL] topic=" + topic + " keys=" + payload.keySet());
  }

  private void fallback(String topic, Map<String, Object> payload, Throwable t) throws Exception {
    throw new Exception("email provider unavailable");
  }
}
