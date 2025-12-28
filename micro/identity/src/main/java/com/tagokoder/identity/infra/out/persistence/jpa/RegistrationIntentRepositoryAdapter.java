package com.tagokoder.identity.infra.out.persistence.jpa;

import com.tagokoder.identity.domain.model.RegistrationIntent;
import com.tagokoder.identity.domain.port.out.RegistrationIntentRepositoryPort;
import com.tagokoder.identity.infra.out.persistence.jpa.mapper.RegistrationIntentJpaMapper;
import org.springframework.stereotype.Component;

@Component
public class RegistrationIntentRepositoryAdapter implements RegistrationIntentRepositoryPort {

    private final SpringDataRegistrationIntentJpa jpa;
    private final RegistrationIntentJpaMapper mapper;

    public RegistrationIntentRepositoryAdapter(SpringDataRegistrationIntentJpa jpa,
                                               RegistrationIntentJpaMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public RegistrationIntent save(RegistrationIntent registrationIntent) {
        RegistrationIntentEntity entity = mapper.fromDomain(registrationIntent);
        RegistrationIntentEntity saved = jpa.save(entity);
        return mapper.toDomain(saved);
    }
}