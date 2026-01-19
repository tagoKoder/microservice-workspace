package com.tagokoder.identity.infra.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagokoder.identity.domain.port.out.AuditPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class EventBridgeAuditPublisher implements AuditPublisher {
  private static final Logger log = LoggerFactory.getLogger(EventBridgeAuditPublisher.class);

  private final EventBridgeClient eb;
  private final ObjectMapper om;
  private final String busName;
  private final String source; // e.g. "bank.identity"

  public EventBridgeAuditPublisher(EventBridgeClient eb, ObjectMapper om, String busName, String source) {
    this.eb = eb;
    this.om = om;
    this.busName = busName;
    this.source = source;
  }

  @Override
  public void publish(AuditEventV1 evt) {
    try {
      String detail = om.writeValueAsString(evt);
      var entry = PutEventsRequestEntry.builder()
          .eventBusName(busName)
          .source(source)
          .detailType("AuditEventV1")
          .detail(detail)
          .build();

      eb.putEvents(PutEventsRequest.builder().entries(entry).build());
    } catch (Exception e) {
      // fallback a log (best-effort)
      log.warn("audit_publish_failed correlation_id={} action={} err={}",
          evt.correlation_id(), evt.action(), e.toString());
    }
  }
}
