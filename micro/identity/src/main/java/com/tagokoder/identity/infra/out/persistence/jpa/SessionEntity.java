package com.tagokoder.identity.infra.out.persistence.jpa;

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

    @Column(name = "refresh_jti", unique = true)
    private String refreshJti;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    private String ip;  // PostgreSQL 'inet' → String
    private String ua;  // User-Agent
}
