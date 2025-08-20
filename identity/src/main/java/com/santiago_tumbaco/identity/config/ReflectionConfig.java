package com.santiago_tumbaco.identity.config;


import io.grpc.protobuf.services.ProtoReflectionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.ServerBuilderCustomizer; // <- paquete correcto

@Configuration
public class ReflectionConfig {

  @Bean
  public ServerBuilderCustomizer addGrpcReflection() {
    return serverBuilder -> serverBuilder.addService(ProtoReflectionService.newInstance());
  }
}