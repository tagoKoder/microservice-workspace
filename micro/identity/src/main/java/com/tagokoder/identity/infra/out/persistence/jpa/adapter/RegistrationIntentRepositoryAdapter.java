package com.tagokoder.identity.infra.out.persistence.jpa.adapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tagokoder.identity.domain.model.RegistrationIntent;
import com.tagokoder.identity.domain.model.kyc.FinalizedObject;
import com.tagokoder.identity.domain.port.out.RegistrationIntentRepositoryPort;
import com.tagokoder.identity.infra.out.persistence.jpa.SpringDataRegistrationIntentJpa;
import com.tagokoder.identity.infra.out.persistence.jpa.entity.RegistrationIntentEntity;
import com.tagokoder.identity.infra.out.persistence.jpa.entity.RegistrationKycObjectEntity;
import com.tagokoder.identity.infra.out.persistence.jpa.mapper.RegistrationIntentJpaMapper;

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
    @Transactional
    public RegistrationIntent save(RegistrationIntent domain) {
        UUID id = domain.getId();
        RegistrationIntentEntity entity = jpa.findById(id)
            .orElseGet(() -> {
                // crear entity nueva SOLO una vez
                RegistrationIntentEntity e = new RegistrationIntentEntity();
                e.setId(id);
                return e;
            });

        // Update campos simples (sin tocar kycObjects)
        mapper.updateEntityFromDomain(domain, entity);

        // Merge de KYC objects por (kind, stage=FINAL)
        mergeFinalKycObjects(domain, entity);

        RegistrationIntentEntity saved = jpa.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RegistrationIntent> findById(UUID registrationId) {
        return jpa.findById(registrationId).map(mapper::toDomain);
    }

        private void mergeFinalKycObjects(RegistrationIntent domain, RegistrationIntentEntity entity) {
        List<FinalizedObject> incoming = domain.getKycObjects() == null ? List.of() : domain.getKycObjects();

        // index existentes FINAL por kind
        Map<String, RegistrationKycObjectEntity> existingByKind = new HashMap<>();
        for (RegistrationKycObjectEntity e : entity.getKycObjects()) {
            if (e.getStage() == RegistrationKycObjectEntity.Stage.FINAL) {
                existingByKind.put(e.getKind().name(), e);
            }
        }

        // kinds que deben quedar
        Set<String> keepKinds = new HashSet<>();

        for (FinalizedObject f : incoming) {
            String k = f.kind().name();
            keepKinds.add(k);

            RegistrationKycObjectEntity child = existingByKind.get(k);

            if (child == null) {
                // nuevo hijo FINAL
                child = mapper.fromDomain(f); // stage FINAL + objectKey ya mapeado
                child.setRegistration(entity); // ✅ NOT NULL
                entity.getKycObjects().add(child);
            } else {
                // update in-place para NO violar unique constraints
                child.setBucket(f.bucket());
                child.setObjectKey(f.key());
                child.setEtag(f.etag());
                child.setContentLength(f.contentLength());
                child.setContentType(f.contentType());
                // kind y stage ya existen
            }
        }

        // eliminar FINAL que ya no están (si aplica en tu negocio)
        entity.getKycObjects().removeIf(e ->
            e.getStage() == RegistrationKycObjectEntity.Stage.FINAL
            && !keepKinds.contains(e.getKind().name())
        );
    }

    
}