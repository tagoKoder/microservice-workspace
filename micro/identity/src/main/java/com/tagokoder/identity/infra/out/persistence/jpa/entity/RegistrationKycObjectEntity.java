package com.tagokoder.identity.infra.out.persistence.jpa.entity;

import java.time.Instant;

import com.tagokoder.identity.domain.model.kyc.KycDocumentKind;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Table(
    name = "registration_kyc_objects",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_reg_kind_stage", columnNames = {"registration_id", "kind", "stage"})
    },
    indexes = {
        @Index(name = "idx_kyc_registration_id", columnList = "registration_id"),
        @Index(name = "idx_kyc_bucket_key", columnList = "bucket, object_key")
    }
)
@Data
public class RegistrationKycObjectEntity {

    public enum Stage {
        STAGING,
        FINAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false)
    private RegistrationIntentEntity registration;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 32)
    private KycDocumentKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 16)
    private Stage stage;

    @Column(name = "bucket", nullable = false, length = 128)
    private String bucket;

    @Column(name = "object_key", nullable = false, length = 1024)
    private String objectKey;

    @Column(name = "etag", length = 128)
    private String etag;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "content_length")
    private Long contentLength;

    // opcional: útil si decides persistir la expiración del presigned emitido
    @Column(name = "expires_at")
    private Instant expiresAt;

    // opcional: útil para auditoría/forense
    @Column(name = "max_bytes")
    private Long maxBytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
