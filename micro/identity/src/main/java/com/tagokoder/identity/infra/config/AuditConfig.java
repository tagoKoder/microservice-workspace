package com.tagokoder.identity.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tagokoder.identity.domain.port.out.AuditPublisher;
import com.tagokoder.identity.infra.audit.EventBridgeAuditPublisher;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import com.fasterxml.jackson.databind.SerializationFeature;

@Configuration
public class AuditConfig {
  @Bean
  public JsonMapper auditJsonMapper() {
    return JsonMapper.builder()
        .findAndAddModules() // ✅ carga JavaTimeModule si está en classpath
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // ✅ ISO-8601
        .build();
  }

  @Bean
  public AuditPublisher auditPublisher(
      EventBridgeClient eb,
      JsonMapper auditJsonMapper,
      @Value("${identity.audit.bus-name}") String busName,
      @Value("${identity.audit.source}") String source
  ) {
    return new EventBridgeAuditPublisher(eb, auditJsonMapper, busName, source);
  }
}
