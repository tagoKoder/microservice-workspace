package com.tagokoder.ops.application.service;

import com.tagokoder.ops.infra.config.OpsProperties;
import com.tagokoder.ops.domain.model.Channel;
import com.tagokoder.ops.domain.model.NotificationEvent;
import com.tagokoder.ops.domain.port.in.DispatchNotificationsUseCase;
import com.tagokoder.ops.domain.port.out.NotificationEventRepositoryPort;
import com.tagokoder.ops.domain.port.out.NotificationPrefRepositoryPort;
import com.tagokoder.ops.domain.port.out.NotificationSenderPort;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class NotificationDispatcherService implements DispatchNotificationsUseCase {

  private final NotificationEventRepositoryPort events;
  private final NotificationPrefRepositoryPort prefs;
  private final Map<Channel, NotificationSenderPort> senders;
  private final OpsProperties props;

  public NotificationDispatcherService(
      NotificationEventRepositoryPort events,
      NotificationPrefRepositoryPort prefs,
      List<NotificationSenderPort> senders,
      OpsProperties props
  ) {
    this.events = events;
    this.prefs = prefs;
    this.senders = senders.stream().collect(Collectors.toMap(NotificationSenderPort::channel, s -> s));
    this.props = props;
  }

  @Override
  public void dispatchOnce() {
    int limit = props.getNotifications().getDispatcher().getBatchSize();
    List<NotificationEvent> batch = events.fetchNextDueForUpdate(limit, Instant.now());
    for (NotificationEvent e : batch) {
      handleOne(e);
    }
  }

  private void handleOne(NotificationEvent e) {
    try {
      Channel channel = resolveChannel(e);
      NotificationSenderPort sender = senders.get(channel);
      if (sender == null) throw new IllegalStateException("sender not configured for " + channel);

      sender.send(e.topic(), e.payload());
      events.markSent(e.id());
    } catch (Exception ex) {
      int nextRetry = e.retryCount() + 1;
      boolean terminal = nextRetry >= props.getNotifications().getDispatcher().getMaxRetries();
      Instant nextAt = terminal ? null : Instant.now().plusMillis(calcBackoffMs(nextRetry));
      events.markFailed(e.id(), nextRetry, nextAt, sanitizeError(ex), terminal);
    }
  }

  private Channel resolveChannel(NotificationEvent e) {
    if (e.channelOverride() != null) return e.channelOverride();

    // payload.customer_id define prefs
    Object cid = e.payload().get("customer_id");
    if (cid instanceof String s && !s.isBlank()) {
      UUID customerId = UUID.fromString(s);
      // regla simple: primer canal opt_in true (prioridad email->sms->push)
      if (prefs.findByCustomerAndChannel(customerId, Channel.EMAIL).map(p -> p.optIn()).orElse(false)) return Channel.EMAIL;
      if (prefs.findByCustomerAndChannel(customerId, Channel.SMS).map(p -> p.optIn()).orElse(false)) return Channel.SMS;
      if (prefs.findByCustomerAndChannel(customerId, Channel.PUSH).map(p -> p.optIn()).orElse(false)) return Channel.PUSH;
    }

    // default seguro (puedes ajustar): EMAIL
    return Channel.EMAIL;
  }

  private long calcBackoffMs(int retryCount) {
    long base = props.getNotifications().getDispatcher().getBaseBackoffMs();
    long max = props.getNotifications().getDispatcher().getMaxBackoffMs();
    long exp = (long) (base * Math.pow(2, Math.min(retryCount, 10)));
    return Math.min(exp, max);
  }

  private String sanitizeError(Exception ex) {
    // ASVS V8/V10: no leaks (no payload dump, no secrets)
    String msg = ex.getMessage();
    if (msg == null) return ex.getClass().getSimpleName();
    return msg.length() > 500 ? msg.substring(0, 500) : msg;
  }
}
