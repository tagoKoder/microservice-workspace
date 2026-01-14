package com.tagokoder.identity.infra.config;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;

@Configuration
public class GrpcServerConfig {

  @Value("${grpc.server.port:9091}")
  private int grpcPort;

  @Bean
  public Server grpcServer(List<io.grpc.BindableService> services) throws IOException {
    NettyServerBuilder builder = NettyServerBuilder.forPort(grpcPort);
    for (var svc : services) builder.addService(svc);
    return builder.build();
  }

  @Bean
  public GrpcServerLifecycle grpcServerLifecycle(Server server) {
    return new GrpcServerLifecycle(server);
  }
}
