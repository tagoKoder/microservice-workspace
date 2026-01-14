package com.tagokoder.identity.infra.out.persistence.jpa.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "identities",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_identities_subject_provider",
        columnNames = {"subject_id_oidc", "provider"}
    )
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdentityEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "subject_id_oidc", nullable = false, length = 255)
    private String subjectIdOidc;

    @Column(name = "provider", nullable = false, length = 64)
    private String provider;

    @Column(name = "user_status", nullable = false, length = 32)
    private String userStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "identity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MfaFactorEntity> mfaFactors = new ArrayList<>();

    @OneToMany(mappedBy = "identity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionEntity> sessions = new ArrayList<>();
}
