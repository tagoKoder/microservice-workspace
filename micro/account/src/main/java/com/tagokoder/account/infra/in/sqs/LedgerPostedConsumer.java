package com.tagokoder.account.infra.in.sqs;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagokoder.account.domain.port.out.InboxRepositoryPort;
import com.tagokoder.account.infra.config.AppProps;
import com.tagokoder.account.infra.out.persistence.jpa.SpringDataAccountBalanceJpa;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

@Service
@ConditionalOnProperty(name = "messaging.sqs.enabled", havingValue = "true", matchIfMissing = false)
public class LedgerPostedConsumer {

  private final SqsClient sqs;
  private final AppProps props;
  private final InboxRepositoryPort inbox;
  private final SpringDataAccountBalanceJpa balances;
  private final ObjectMapper om = new ObjectMapper();

  public LedgerPostedConsumer(
    SqsClient sqs,
    AppProps props,
    InboxRepositoryPort inbox,
    SpringDataAccountBalanceJpa balances
  ) {
    this.sqs = sqs;
    this.props = props;
    this.inbox = inbox;
    this.balances = balances;
  }

  @Scheduled(fixedDelayString = "2000")
  @Transactional
  public void poll() throws Exception {

    var resp = sqs.receiveMessage(r -> r
      .queueUrl(props.aws().ledgerEventsQueueUrl())
      .waitTimeSeconds(20)
      .maxNumberOfMessages(10)
    );

    for (Message m : resp.messages()) {
      try {
        LedgerPostedDetail detail = unwrapEventBridgeDetail(m.body());
        String eventId = (detail.eventId() != null && !detail.eventId().isBlank())
          ? detail.eventId()
          : extractEventIdSafe(m.body());

        // idempotencia de consumo
        boolean canProcess = inbox.tryBegin(eventId, "ledger.journal.posted");
        if (!canProcess) {
          sqs.deleteMessage(d -> d.queueUrl(props.aws().ledgerEventsQueueUrl()).receiptHandle(m.receiptHandle()));
          continue;
        }

        // aplica postings
        for (Posting p : detail.postings()) {
          balances.applyDeltas(
            UUID.fromString(p.accountId()),
            new BigDecimal(p.dLedger()),
            new BigDecimal(p.dAvailable()),
            new BigDecimal(p.dHold())
          );
        }

        inbox.markProcessed(eventId);
        sqs.deleteMessage(d -> d.queueUrl(props.aws().ledgerEventsQueueUrl()).receiptHandle(m.receiptHandle()));

      } catch (Exception e) {
        inbox.markFailedSafe(extractEventIdSafe(m.body()), "ledger.journal.posted", e.toString());
        throw e; // rollback como pediste
      }
    }
  }

  /**
   * SQS Body:
   * - Si viene de SNS subscription, body es el JSON del envelope SNS con field "Message" (string).
   * - Message contiene el JSON del evento de EventBridge.
   * - EventBridge tiene field "detail" (objeto o string JSON).
   */
  private LedgerPostedDetail unwrapEventBridgeDetail(String sqsBody) throws Exception {
    JsonNode root = om.readTree(sqsBody);

    // Caso SNS envelope
    if (root.has("Type") && root.has("Message")) {
      String msg = root.get("Message").asText();
      return unwrapFromEventBridge(msg);
    }

    // Caso directo EventBridge en SQS
    return unwrapFromEventBridge(sqsBody);
  }

  private LedgerPostedDetail unwrapFromEventBridge(String eventBridgeJson) throws Exception {
    JsonNode eb = om.readTree(eventBridgeJson);

    String ebId = eb.has("id") ? eb.get("id").asText(null) : null;

    JsonNode detailNode = eb.get("detail");
    if (detailNode == null) throw new IllegalArgumentException("EventBridge detail missing");

    // detail puede venir como string
    JsonNode d = detailNode.isTextual() ? om.readTree(detailNode.asText()) : detailNode;

    String detailEventId = d.has("event_id") ? d.get("event_id").asText(null) : null;
    String eventId = (detailEventId != null && !detailEventId.isBlank()) ? detailEventId : ebId;

    if (!d.has("postings") || !d.get("postings").isArray()) {
      throw new IllegalArgumentException("detail.postings missing/invalid");
    }

    List<Posting> postings = om.readerForListOf(Posting.class).readValue(d.get("postings"));

    return new LedgerPostedDetail(eventId, postings);
  }

  private String extractEventIdSafe(String sqsBody) {
    try {
      JsonNode root = om.readTree(sqsBody);
      if (root.has("Message")) {
        JsonNode eb = om.readTree(root.get("Message").asText());
        if (eb.has("id")) return eb.get("id").asText();
      }
      if (root.has("id")) return root.get("id").asText();
      return null;
    } catch (Exception ignored) {
      return null;
    }
  }

  // DTOs mínimos
  public record LedgerPostedDetail(String eventId, List<Posting> postings) {}

  public static class Posting {
    private String account_id;
    private String d_ledger;
    private String d_available;
    private String d_hold;

    public String accountId() { return account_id; }
    public String dLedger() { return d_ledger; }
    public String dAvailable() { return d_available; }
    public String dHold() { return d_hold; }

    // setters para Jackson
    public void setAccount_id(String v) { this.account_id = v; }
    public void setD_ledger(String v) { this.d_ledger = v; }
    public void setD_available(String v) { this.d_available = v; }
    public void setD_hold(String v) { this.d_hold = v; }
  }
}
