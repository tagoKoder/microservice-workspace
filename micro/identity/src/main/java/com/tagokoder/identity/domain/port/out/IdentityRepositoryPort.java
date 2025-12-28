package com.tagokoder.identity.domain.port.out;
import java.util.Optional;
import java.util.UUID;

import com.tagokoder.identity.domain.model.Identity;

public interface IdentityRepositoryPort {
    Optional<Identity> findBySubjectAndProvider(String sub, String provider);
    Identity save(Identity identity);
    Optional<Identity> findById(UUID id);
}
