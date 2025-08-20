package com.santiago_tumbaco.identity.adapter.observability;


import io.sentry.Sentry;
import io.sentry.SentryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class SentryConfig {

  @Value("${SENTRY_DSN:}")
  String dsn;

  @Value("${SENTRY_ENV:dev}")
  String env;

  @Value("${SENTRY_RELEASE:identity-service@local}")
  String release;

  @Value("${SERVICE_NAME:identity-service}")
  String service;

  @PostConstruct
  public void init() {
    if (dsn == null || dsn.isBlank()) return;
    Sentry.init((SentryOptions options) -> {
      options.setDsn(dsn);
      options.setEnvironment(env);
      options.setRelease(release);
      options.setServerName(service);
      options.setAttachStacktrace(true);
      // Puedes añadir un BeforeSend para más control/sanitización
    });
  }
}