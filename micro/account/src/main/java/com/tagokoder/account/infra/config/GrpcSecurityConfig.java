package com.tagokoder.account.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.tagokoder.account.domain.port.out.AccountRepositoryPort;
import com.tagokoder.account.domain.port.out.IdentityPrincipalPort;
import com.tagokoder.account.infra.out.audit.AuditPublisher;
import com.tagokoder.account.infra.security.authz.AccountResourceTemplates;
import com.tagokoder.account.infra.security.authz.RouteAuthzRegistry;
import com.tagokoder.account.infra.security.avp.AvpAuthorizer;
import com.tagokoder.account.infra.security.grpc.AuthzServerInterceptor;

@Configuration
public class GrpcSecurityConfig {

  @Bean
  RouteAuthzRegistry routeAuthzRegistry() {
    return new RouteAuthzRegistry();
  }

  @Bean
  AccountResourceTemplates accountResourceTemplates(AccountRepositoryPort accountRepo) {
    return new AccountResourceTemplates(accountRepo);
  }

  @Bean
  AuthzServerInterceptor authzServerInterceptor(
      AppProps props,
      JwtDecoder jwtDecoder,
      AvpAuthorizer avp,
      AuditPublisher audit,
      RouteAuthzRegistry registry,
      AccountResourceTemplates templates,
      IdentityPrincipalPort principalPort
  ) {
    return new AuthzServerInterceptor(props, jwtDecoder, avp, audit, registry, templates, principalPort);
  }
}