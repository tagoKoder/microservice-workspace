package com.tagokoder.identity.infra.out.persistence.jpa.adapter;

import com.tagokoder.identity.domain.model.RegistrationIntent;
import com.tagokoder.identity.domain.port.out.RegistrationIntentRepositoryPort;
import com.tagokoder.identity.infra.out.persistence.jpa.SpringDataRegistrationIntentJpa;
import com.tagokoder.identity.infra.out.persistence.jpa.entity.RegistrationIntentEntity;
import com.tagokoder.identity.infra.out.persistence.jpa.mapper.RegistrationIntentJpaMapper;

import java.util.Optional;
import java.util.UUID;

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

    @Override
    public Optional<RegistrationIntent> findById(UUID registrationId) {
        return jpa.findById(registrationId).map(mapper::toDomain);
    }

    
}