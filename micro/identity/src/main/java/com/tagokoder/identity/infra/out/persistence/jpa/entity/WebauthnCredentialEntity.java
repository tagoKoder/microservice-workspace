package com.tagokoder.identity.infra.out.persistence.jpa.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
    name = "webauthn_credentials",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_webauthn_credential_id",
        columnNames = {"credential_id"}
    )
)
@Data
public class WebauthnCredentialEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "identity_id", nullable = false)
    private IdentityEntity identity;

    @Column(name = "credential_id", nullable = false)
    private String credentialId; // base64url

    @Lob
    @Column(name = "public_key_cose", nullable = false)
    private byte[] publicKeyCose;

    @Column(name = "sign_count", nullable = false)
    private long signCount;

    @Column(name = "aaguid")
    private UUID aaguid;

    @Column(name = "transports")
    private String transports;

    @Column(name = "name")
    private String name;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;
}
