package com.tagokoder.identity.infra.out.persistence.jpa;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name="identities")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdentityEntity {
    @Id
    private UUID id= UUID.randomUUID();
    @OneToMany(mappedBy = "identity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MfaFactorEntity> mfaFactors = new ArrayList<>();

    @OneToMany(mappedBy = "identity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionEntity> sessions = new ArrayList<>();

    @Column(name="subject_id_oidc", unique=true, nullable=false)
    private String subjectIdOidc;
    private String provider;
    @Column(name="user_status")
    private String userStatus;
    @Column(name="created_at")
    private LocalDateTime createdAt;
}
