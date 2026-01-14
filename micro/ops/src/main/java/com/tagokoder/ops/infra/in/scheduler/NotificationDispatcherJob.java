package com.tagokoder.ops.infra.in.scheduler;

import com.tagokoder.ops.infra.config.OpsProperties;
import com.tagokoder.ops.domain.port.in.DispatchNotificationsUseCase;
import org.springframework.scheduling.annotation.Scheduled;

public class NotificationDispatcherJob {

  private final DispatchNotificationsUseCase dispatcher;
  private final OpsProperties props;

  public NotificationDispatcherJob(DispatchNotificationsUseCase dispatcher, OpsProperties props) {
    this.dispatcher = dispatcher;
    this.props = props;
  }

  @Scheduled(fixedDelayString = "${ops.notifications.dispatcher.pollIntervalMs}")
  public void tick() {
    dispatcher.dispatchOnce();
  }
}
