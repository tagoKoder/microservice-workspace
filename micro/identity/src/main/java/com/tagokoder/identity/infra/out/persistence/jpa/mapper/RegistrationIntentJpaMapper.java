package com.tagokoder.identity.infra.out.persistence.jpa.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.tagokoder.identity.domain.model.RegistrationIntent;
import com.tagokoder.identity.domain.model.kyc.FinalizedObject;
import com.tagokoder.identity.infra.config.MappingConfig;
import com.tagokoder.identity.infra.out.persistence.jpa.entity.RegistrationIntentEntity;
import com.tagokoder.identity.infra.out.persistence.jpa.entity.RegistrationKycObjectEntity;

@Mapper(config = MappingConfig.class)
public interface RegistrationIntentJpaMapper {

    // -------------------------
    // State String <-> Enum
    // -------------------------
    default RegistrationIntent.State mapState(String s) {
        if (s == null || s.isBlank()) return null;
        return RegistrationIntent.State.valueOf(s);
    }

    default String mapState(RegistrationIntent.State s) {
        return s == null ? null : s.name();
    }

    // -------------------------
    // Intent (raíz)
    // -------------------------
    @Mapping(target = "state", expression = "java(mapState(entity.getState()))")
    RegistrationIntent toDomain(RegistrationIntentEntity entity);

    @Mapping(target = "state", expression = "java(mapState(domain.getState()))")
    RegistrationIntentEntity fromDomain(RegistrationIntent domain);

    /**
     * ✅ Update in-place para NO recrear todo y evitar inserts duplicados.
     * Importante: ignoramos kycObjects aquí y los mergeamos manualmente.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "kycObjects", ignore = true)
    @Mapping(target = "state", expression = "java(mapState(domain.getState()))")
    void updateEntityFromDomain(RegistrationIntent domain, @MappingTarget RegistrationIntentEntity target);

    // -------------------------
    // KYC Object (hijo)
    // -------------------------
    @Mapping(target = "key", source = "objectKey")
    FinalizedObject toDomain(RegistrationKycObjectEntity entity);

    @Mapping(target = "objectKey", source = "key")
    @Mapping(target = "stage", constant = "FINAL")       // ✅ NOT NULL + negocio: confirmado = FINAL
    @Mapping(target = "registration", ignore = true)     // lo seteamos afuera
    @Mapping(target = "id", ignore = true)               // lo maneja DB
    @Mapping(target = "createdAt", ignore = true)        // @PrePersist
    @Mapping(target = "updatedAt", ignore = true)        // @PreUpdate
    @Mapping(target = "expiresAt", ignore = true)        // no está en domain FinalizedObject
    @Mapping(target = "maxBytes", ignore = true)         // no está en domain FinalizedObject
    RegistrationKycObjectEntity fromDomain(FinalizedObject domain);

    @AfterMapping
    default void linkChildren(@MappingTarget RegistrationIntentEntity target) {
        if (target.getKycObjects() != null) {
            for (RegistrationKycObjectEntity k : target.getKycObjects()) {
                k.setRegistration(target); // ✅ el campo real es "registration"
            }
        }
    }
}
