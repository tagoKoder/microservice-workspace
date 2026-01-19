package com.tagokoder.identity.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import com.tagokoder.identity.domain.model.RegistrationIntent;

public interface RegistrationIntentRepositoryPort {
    RegistrationIntent save(RegistrationIntent registrationIntent);
    Optional<RegistrationIntent> findById(UUID registrationId);
}
