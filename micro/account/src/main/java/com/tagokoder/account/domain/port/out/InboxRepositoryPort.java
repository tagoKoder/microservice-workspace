package com.tagokoder.account.domain.port.out;

public interface InboxRepositoryPort {
  boolean tryBegin(String eventId, String eventType); // inserta si no existe
  void markProcessed(String eventId);
  void markFailedSafe(String eventIdOrNull, String eventType, String error);
}
