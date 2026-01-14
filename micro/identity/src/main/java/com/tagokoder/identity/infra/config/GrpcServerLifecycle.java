package com.tagokoder.identity.infra.config;

import io.grpc.Server;
import org.springframework.context.SmartLifecycle;

public class GrpcServerLifecycle implements SmartLifecycle {

  private final Server server;
  private boolean running = false;

  public GrpcServerLifecycle(Server server) {
    this.server = server;
  }

  @Override
  public void start() {
    try {
      server.start();
      running = true;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to start gRPC server", e);
    }
  }

  @Override
  public void stop() {
    server.shutdown();
    running = false;
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE;
  }
}
