package com.tagokoder.ops.infra.in.grpc;

import bank.ops.v1.*;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;

import com.tagokoder.ops.domain.port.in.GetNotificationPrefsUseCase;
import com.tagokoder.ops.domain.port.out.DataAccessLogPort;

import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class NotificationsGrpcService extends NotificationsServiceGrpc.NotificationsServiceImplBase {

  private final GetNotificationPrefsUseCase prefs;
  private final DataAccessLogPort accessLog;

  public NotificationsGrpcService(GetNotificationPrefsUseCase prefs, DataAccessLogPort accessLog) {
    this.prefs = prefs;
    this.accessLog = accessLog;
  }

  @Override
  public void getNotificationPrefs(GetNotificationPrefsRequest request,
                                  StreamObserver<GetNotificationPrefsResponse> responseObserver) {

    String subject = request.hasXSubject() ? request.getXSubject().getValue() : "internal";
    UUID customerId = UUID.fromString(request.getCustomerId());

    accessLog.log(
        subject,
        "READ",
        "notification_prefs",
        customerId,
        "customer_prefs_lookup",
        Instant.now()
    );

    var items = prefs.get(customerId).stream()
        .map(p -> NotificationPrefItem.newBuilder()
            .setChannel(mapChannel(p.channel().toWire()))
            .setOptIn(p.optIn())
            .setUpdatedAt(toTimestamp(p.updatedAt()))
            .build()
        )
        .toList();

    var response = GetNotificationPrefsResponse.newBuilder()
        .addAllPrefs(items)
        .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  private NotificationChannel mapChannel(String wire) {
    if (wire == null) return NotificationChannel.NOTIFICATION_CHANNEL_UNSPECIFIED;
    return switch (wire) {
      case "email" -> NotificationChannel.NOTIFICATION_CHANNEL_EMAIL;
      case "sms" -> NotificationChannel.NOTIFICATION_CHANNEL_SMS;
      case "push" -> NotificationChannel.NOTIFICATION_CHANNEL_PUSH;
      default -> NotificationChannel.NOTIFICATION_CHANNEL_UNSPECIFIED;
    };
  }

  private Timestamp toTimestamp(Instant instant) {
    if (instant == null) {
      return Timestamp.newBuilder().setSeconds(0).setNanos(0).build();
    }
    return Timestamp.newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();
  }
}
