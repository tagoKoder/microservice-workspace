package com.tagokoder.identity.domain.port.out;

import com.tagokoder.identity.domain.model.RegistrationIntent;

public interface RegistrationIntentRepositoryPort {
    RegistrationIntent save(RegistrationIntent registrationIntent);
}
