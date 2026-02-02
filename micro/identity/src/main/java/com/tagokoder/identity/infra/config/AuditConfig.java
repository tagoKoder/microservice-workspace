package com.tagokoder.identity.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagokoder.identity.domain.port.out.AuditPublisher;
import com.tagokoder.identity.infra.audit.EventBridgeAuditPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Configuration
public class AuditConfig {

  @Bean
  public AuditPublisher auditPublisher(
      EventBridgeClient eb,
      ObjectMapper om,
      @Value("${identity.audit.bus-name}") String busName,
      @Value("${identity.audit.source}") String source
  ) {
    return new EventBridgeAuditPublisher(eb, om, busName, source);
  }
}
