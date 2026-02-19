package com.tagokoder.identity.infra.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import com.tagokoder.identity.infra.security.grpc.CorrelationServerInterceptor;

import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GlobalServerInterceptorConfigurer;

@Configuration
public class GrpcInterceptorsConfig implements GlobalServerInterceptorConfigurer {

  private final CorrelationServerInterceptor corr;
  private final ServerInterceptor authz;

  public GrpcInterceptorsConfig(
      CorrelationServerInterceptor corr,
      @Qualifier("authzInterceptor") ServerInterceptor authz) {
    this.corr = corr;
    this.authz = authz;
  }

  @Override
  public void configureServerInterceptors(List<ServerInterceptor> interceptors) {
    // Orden recomendado: correlation -> authz
    interceptors.add(corr);
    interceptors.add(authz);
  }
}
