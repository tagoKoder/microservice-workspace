package com.santiago_tumbaco.identity.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GlobalServerInterceptor;   // <- del starter oficial
import io.grpc.ServerInterceptor;

@Configuration
public class GrpcConfig {

  @Bean
  @GlobalServerInterceptor
  public ServerInterceptor metadataInterceptor() {
    return new MetadataInterceptor();
  }
}