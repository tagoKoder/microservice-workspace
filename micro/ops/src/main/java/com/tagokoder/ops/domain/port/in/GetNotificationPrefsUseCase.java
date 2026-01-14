package com.tagokoder.ops.domain.port.in;

import com.tagokoder.ops.domain.model.NotificationPref;
import java.util.List;
import java.util.UUID;

public interface GetNotificationPrefsUseCase {
  List<NotificationPref> get(UUID customerId);
}
