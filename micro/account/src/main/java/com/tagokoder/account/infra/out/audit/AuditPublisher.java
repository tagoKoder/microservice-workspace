package com.tagokoder.account.infra.out.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagokoder.account.infra.config.AppProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.time.Instant;
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

    public void publish(Map<String, Object> event) {
        try {
            String detail = om.writeValueAsString(event);

            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                    .eventBusName(props.aws().auditBusName())
                    .source("bank.account")
                    .detailType("AuditEvent")
                    .time(Instant.now())
                    .detail(detail)
                    .build();

            eb.putEvents(PutEventsRequest.builder().entries(entry).build());
        } catch (Exception e) {
            // best-effort fallback
            log.warn("audit.publish_failed correlation_id={} err={}",
                    event.get("correlation_id"), e.toString());
        }
    }

    public static Map<String, Object> base(String service, String env, String correlationId) {
        return Map.of(
                "event_version", "1.0",
                "event_id", UUID.randomUUID().toString(),
                "occurred_at", Instant.now().toString(),
                "service", service,
                "environment", env,
                "correlation_id", correlationId
        );
    }
}
