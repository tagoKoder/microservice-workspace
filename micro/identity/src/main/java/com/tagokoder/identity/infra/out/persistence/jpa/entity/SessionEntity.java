package com.tagokoder.identity.infra.out.persistence.jpa.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionEntity {
    @Id
    @Column(name = "session_id")
    private UUID sessionId;

    @ManyToOne
    @JoinColumn(name = "identity_id", nullable = false)
    private IdentityEntity identity;

    // Hash HMAC(refresh_token) para verificación/rotación/auditoría (NO reversible)
    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    // Refresh token cifrado (AES-GCM) para poder hacer refresh server-side
    @Column(name = "refresh_token_enc", nullable = false, columnDefinition = "text")
    private String refreshTokenEnc;


    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    @Column(name = "absolute_expires_at")
    private LocalDateTime absoluteExpiresAt;

    @Column(name = "rotated_to_session_id")
    private UUID rotatedToSessionId;
    @Column(name = "access_token_enc", columnDefinition = "text")
    private String accessTokenEnc;

    @Column(name = "access_token_expires_at")
    private LocalDateTime accessTokenExpiresAt;
    @Column(name = "ip", columnDefinition = "text")
    private String ip; 
    @Column(name = "ua", columnDefinition = "text")
    private String ua;  // User-Agent
}
