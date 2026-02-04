package com.tagokoder.identity.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tagokoder.identity.domain.port.out.AuditPublisher;
import com.tagokoder.identity.infra.audit.EventBridgeAuditPublisher;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Configuration
public class AuditConfig {
  private JsonMapper om = new JsonMapper();
  @Bean
  public AuditPublisher auditPublisher(
      EventBridgeClient eb,
      @Value("${identity.audit.bus-name}") String busName,
      @Value("${identity.audit.source}") String source
  ) {
    return new EventBridgeAuditPublisher(eb, om, busName, source);
  }
}
