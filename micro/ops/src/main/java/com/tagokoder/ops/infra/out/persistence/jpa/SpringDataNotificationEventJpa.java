package com.tagokoder.ops.infra.out.persistence.jpa;

import com.tagokoder.ops.infra.out.persistence.jpa.entity.NotificationEventEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringDataNotificationEventJpa extends JpaRepository<NotificationEventEntity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
    value = """
      SELECT *
      FROM notification_events
      WHERE status = 'queued'
        AND (next_retry_at IS NULL OR next_retry_at <= :now)
      ORDER BY created_at ASC
      LIMIT :limit
      FOR UPDATE SKIP LOCKED
    """,
    nativeQuery = true
  )
  List<NotificationEventEntity> fetchNextDueForUpdate(@Param("now") Instant now, @Param("limit") int limit);
}
