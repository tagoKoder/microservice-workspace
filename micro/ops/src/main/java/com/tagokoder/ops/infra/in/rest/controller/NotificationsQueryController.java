package com.tagokoder.ops.infra.in.rest.controller;

import com.tagokoder.ops.domain.port.in.GetNotificationPrefsUseCase;
import com.tagokoder.ops.domain.port.out.DataAccessLogPort;
import com.tagokoder.ops.infra.in.rest.dto.NotificationPrefsResponseDto;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationsQueryController {

  private final GetNotificationPrefsUseCase prefs;
  private final DataAccessLogPort accessLog;

  public NotificationsQueryController(GetNotificationPrefsUseCase prefs, DataAccessLogPort accessLog) {
    this.prefs = prefs;
    this.accessLog = accessLog;
  }

  @GetMapping("/prefs")
  public NotificationPrefsResponseDto getPrefs(@RequestParam("customer_id") UUID customerId,
                                               @RequestHeader(value = "X-Subject", required = false) String subject) {
    // ASVS V8/V10: data access log
    accessLog.log(subject != null ? subject : "internal",
        "READ", "notification_prefs", customerId, "customer_prefs_lookup", Instant.now());

    var res = prefs.get(customerId).stream()
        .map(p -> new NotificationPrefsResponseDto.PrefDto(p.channel().toWire(), p.optIn(), p.updatedAt().toString()))
        .toList();

    return new NotificationPrefsResponseDto(res);
  }
}
