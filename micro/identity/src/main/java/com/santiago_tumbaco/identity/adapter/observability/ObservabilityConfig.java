package com.santiago_tumbaco.identity.adapter.observability;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

@Configuration
public class ObservabilityConfig {

  @Bean
  public OpenTelemetry openTelemetry() {
    // Construye el SDK leyendo OTEL_* / properties y lo registra como GLOBAL
    OpenTelemetrySdk sdk = AutoConfiguredOpenTelemetrySdk.builder()
        .setResultAsGlobal()
        .build()
        .getOpenTelemetrySdk();
    return sdk; // OpenTelemetrySdk implementa OpenTelemetry
  }

  @Bean
  public Tracer tracer(OpenTelemetry otel) {
    return otel.getTracer("identity-service");
  }
}
