package com.tagokoder.identity.infra.out.persistence.jpa.adapter;

import com.tagokoder.identity.domain.model.Identity;
import com.tagokoder.identity.domain.port.out.IdentityRepositoryPort;
import com.tagokoder.identity.infra.out.persistence.jpa.SpringDataIdentityJpa;
import com.tagokoder.identity.infra.out.persistence.jpa.mapper.IdentityJpaMapper;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class IdentityRepositoryAdapter implements IdentityRepositoryPort {

    private final SpringDataIdentityJpa jpa;
    private final IdentityJpaMapper mapper;

    public IdentityRepositoryAdapter(SpringDataIdentityJpa jpa, IdentityJpaMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Optional<Identity> findBySubjectAndProvider(String sub, String provider) {
        return jpa.findBySubjectIdOidcAndProvider(sub, provider)
                .map(mapper::toDomain);
    }

    @Override
    public Identity save(Identity identity) {
        return mapper.toDomain(jpa.save(mapper.fromDomain(identity)));
    }

    @Override
    public Optional<Identity> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

}
