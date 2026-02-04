package com.tagokoder.account.infra.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import io.grpc.protobuf.services.HealthStatusManager;

@Component
public class GrpcReadiness {

  private final HealthStatusManager health;

  public GrpcReadiness(HealthStatusManager health) {
    this.health = health;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void ready() {
    // "" significa estado global del servidor
    health.setStatus("", io.grpc.health.v1.HealthCheckResponse.ServingStatus.SERVING);
  }
}

