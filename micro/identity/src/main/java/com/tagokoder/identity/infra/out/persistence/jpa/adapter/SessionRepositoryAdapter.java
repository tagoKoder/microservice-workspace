package com.tagokoder.identity.infra.out.persistence.jpa.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.tagokoder.identity.domain.model.Session;
import com.tagokoder.identity.domain.port.out.SessionRepositoryPort;
import com.tagokoder.identity.infra.out.persistence.jpa.SpringDataIdentityJpa;
import com.tagokoder.identity.infra.out.persistence.jpa.SpringDataSessionJpa;
import com.tagokoder.identity.infra.out.persistence.jpa.entity.SessionEntity;
import com.tagokoder.identity.infra.out.persistence.jpa.mapper.SessionJpaMapper;

@Component
public class SessionRepositoryAdapter implements SessionRepositoryPort {

    private final SpringDataSessionJpa jpa;
    private final SpringDataIdentityJpa identityJpa;
    private final SessionJpaMapper mapper;

    public SessionRepositoryAdapter(SpringDataSessionJpa jpa, SpringDataIdentityJpa identityJpa, SessionJpaMapper mapper) {
        this.jpa = jpa;
        this.identityJpa = identityJpa;
        this.mapper = mapper;
    }

    @Override
    public Optional<Session> findById(UUID sessionId) {
        return jpa.findById(sessionId).map(mapper::toDomain);
    }

    @Override
    public Session save(Session session) {
        SessionEntity entity = mapper.fromDomain(session);

        // referencia (proxy) sin traer toda la entidad (correcto para ManyToOne)
        // Spring recomienda getReferenceById como reemplazo de getOne/getById. :contentReference[oaicite:1]{index=1}
        entity.setIdentity(identityJpa.getReferenceById(session.getIdentityId()));

        SessionEntity saved = jpa.save(entity);
        return mapper.toDomain(saved);
    }
}
