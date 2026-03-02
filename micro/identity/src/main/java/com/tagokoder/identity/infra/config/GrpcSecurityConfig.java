package com.tagokoder.identity.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tagokoder.identity.infra.security.authz.IdentityResourceTemplates;
import com.tagokoder.identity.infra.security.authz.RouteAuthzRegistry;

@Configuration
public class GrpcSecurityConfig {

  @Bean
  public RouteAuthzRegistry routeAuthzRegistry() {
    return new RouteAuthzRegistry();
  }

  @Bean
  public IdentityResourceTemplates identityResourceTemplates() {
    return new IdentityResourceTemplates();
  }
}