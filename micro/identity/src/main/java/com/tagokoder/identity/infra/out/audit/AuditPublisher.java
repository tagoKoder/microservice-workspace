package com.tagokoder.identity.infra.out.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagokoder.identity.infra.config.AppProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuditPublisher {
    private static final Logger log = LoggerFactory.getLogger(AuditPublisher.class);

    private final EventBridgeClient eb;
    private final AppProps props;
    private final ObjectMapper om = new ObjectMapper();

    public AuditPublisher(EventBridgeClient eb, AppProps props) {
        this.eb = eb;
        this.props = props;
    }

    public void publish(Map<String, Object> auditEvent) {
        try {
            String detail = om.writeValueAsString(auditEvent);

            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                    .eventBusName(props.aws().auditBusName())
                    .source("bank.identity")
                    .detailType("AuditEvent")
                    .time(Instant.now())
                    .detail(detail)
                    .build();

            eb.putEvents(PutEventsRequest.builder().entries(entry).build());
        } catch (Exception e) {
            log.warn("audit.publish_failed correlation_id={} err={}",
                    auditEvent.get("correlation_id"), e.toString());
        }
    }

    public Map<String, Object> base(String correlationId, String routeTemplate, String actionId) {
        Map<String, Object> m = new HashMap<>();
        m.put("event_version", "1.0");
        m.put("event_id", UUID.randomUUID().toString());
        m.put("occurred_at", Instant.now().toString());
        m.put("service", "identity");
        m.put("environment", props.env());
        m.put("correlation_id", correlationId);
        m.put("route_template", routeTemplate);
        m.put("action", actionId);
        return m;
    }
}
