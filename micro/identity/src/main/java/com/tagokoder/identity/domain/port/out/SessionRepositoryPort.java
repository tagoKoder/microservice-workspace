package com.tagokoder.identity.domain.port.out;



import java.util.Optional;
import java.util.UUID;

import com.tagokoder.identity.domain.model.Session;

public interface SessionRepositoryPort {
    Optional<Session> findById(UUID sessionId);
    Session save(Session entity);
}
