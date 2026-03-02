package com.tagokoder.identity.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import com.tagokoder.identity.application.OidcProperties;
import com.tagokoder.identity.infra.out.oidc.OidcDiscoveryClient;

@Configuration
public class OidcBeansConfig {

  @Bean
  public OidcDiscoveryClient oidcDiscoveryClient(WebClient oidcWebClient, OidcProperties props) {
    return new OidcDiscoveryClient(oidcWebClient, props);
  }
}