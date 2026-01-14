package com.tagokoder.identity.application.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tagokoder.identity.application.IdentitySecurityProperties;
import com.tagokoder.identity.application.IdentitySessionProperties;
import com.tagokoder.identity.domain.model.Identity;
import com.tagokoder.identity.domain.model.Session;
import com.tagokoder.identity.domain.port.in.CreateSessionUseCase;
import com.tagokoder.identity.domain.port.in.GetSessionInfoUseCase;
import com.tagokoder.identity.domain.port.in.LogoutSessionUseCase;
import com.tagokoder.identity.domain.port.in.RefreshSessionUseCase;
import com.tagokoder.identity.domain.port.out.IdentityRepositoryPort;
import com.tagokoder.identity.domain.port.out.OidcIdpClientPort;
import com.tagokoder.identity.domain.port.out.SessionRepositoryPort;
import com.tagokoder.identity.infra.security.TokenCrypto;
import com.tagokoder.identity.infra.security.TokenHasher;

@Service
public class SessionService implements CreateSessionUseCase, RefreshSessionUseCase, LogoutSessionUseCase, GetSessionInfoUseCase {

    private final SessionRepositoryPort sessions;
    private final OidcIdpClientPort idp;
    private final IdentitySessionProperties sessionProps;
    private final TokenHasher hasher;
    private final TokenCrypto crypto;
    private final IdentityRepositoryPort identities;

    public SessionService(SessionRepositoryPort sessions,
                          OidcIdpClientPort idp,
                          IdentitySessionProperties sessionProps,
                          IdentitySecurityProperties secProps,
                          IdentityRepositoryPort identities) {
        this.sessions = sessions;
        this.idp = idp;
        this.sessionProps = sessionProps;
        this.hasher = new TokenHasher(secProps.getRefreshTokenPepper());
        this.crypto = new TokenCrypto(secProps.getRefreshTokenEncKeyB64());
        this.identities = identities;
    }

    @Override
    public SessionInfo get(UUID sessionId, String ip, String userAgent) {
        var s = sessions.findById(sessionId).orElseThrow(() -> new IllegalStateException("Session not found"));

        var now = Instant.now();
        if (s.getRevokedAt() != null) throw new IllegalStateException("Session revoked");
        if (s.getExpiresAt() != null && s.getExpiresAt().isBefore(now)) throw new IllegalStateException("Session expired");
        if (s.getAbsoluteExpiresAt() != null && s.getAbsoluteExpiresAt().isBefore(now))
            throw new IllegalStateException("Session absolute expiry reached");

        var identity = identities.findById(s.getIdentityId()).orElseThrow(() -> new IllegalStateException("Identity not found"));
        long expiresIn = java.time.Duration.between(now, s.getExpiresAt()).getSeconds();
        if (expiresIn < 0) expiresIn = 0;

        return new SessionInfo(
                identity.getId(),
                identity.getSubjectIdOidc(),
                identity.getProvider(),
                identity.getUserStatus().name(),
                expiresIn
        );
    }


    public CreatedSession createSession(Identity identity,
                                        String refreshToken,
                                        String ip,
                                        String ua, boolean mfaRequired) {
        UUID sid = UUID.randomUUID();
        Instant now = Instant.now();

        Session s = new Session();
        s.setSessionId(sid);
        s.setIdentityId(identity.getId());
        s.setCreatedAt(now);
        s.setExpiresAt(now.plusSeconds(sessionProps.getTtlSeconds()));
        s.setAbsoluteExpiresAt(now.plusSeconds(sessionProps.getAbsoluteTtlSeconds()));
        s.setRevokedAt(null);
        s.setRotatedToSessionId(null);
        s.setIp(ip);
        s.setUa(ua);
        s.setMfaRequired(mfaRequired);
        s.setMfaVerifiedAt(mfaRequired ? null : now);

        s.setRefreshTokenHash(hasher.hmacSha256(refreshToken));
        s.setRefreshTokenEnc(crypto.encrypt(refreshToken));


        sessions.save(s);

        return new CreatedSession(sid, sessionProps.getTtlSeconds());
    }

    public RefreshedSession refresh(UUID sessionId, String ip, String ua) {
        Session cur = sessions.findById(sessionId)
                .orElseThrow(() -> new IllegalStateException("Session not found"));

        Instant now = Instant.now();
        if (cur.getRevokedAt() != null) throw new IllegalStateException("Session revoked");
        if (cur.getExpiresAt() != null && cur.getExpiresAt().isBefore(now)) throw new IllegalStateException("Session expired");
        if (cur.getAbsoluteExpiresAt() != null && cur.getAbsoluteExpiresAt().isBefore(now))
            throw new IllegalStateException("Session absolute expiry reached");

        String refreshToken = crypto.decrypt(cur.getRefreshTokenEnc());

        // Refresh en OP
        var token = idp.refreshTokens(refreshToken);

        // Rotación de refresh token (si OP devuelve uno nuevo, se usa; si no, reusa el actual)
        String newRefresh = (token.refreshToken != null && !token.refreshToken.isBlank()) ? token.refreshToken : refreshToken;

        // Rotación de session_id (anti fixation)
        UUID newSid = UUID.randomUUID();

        Session next = new Session();
        next.setSessionId(newSid);
        next.setIdentityId(cur.getIdentityId());
        next.setCreatedAt(now);
        next.setExpiresAt(now.plusSeconds(sessionProps.getTtlSeconds()));
        next.setAbsoluteExpiresAt(cur.getAbsoluteExpiresAt());
        next.setIp(ip);
        next.setUa(ua);
        next.setRevokedAt(null);
        next.setRotatedToSessionId(null);
        next.setRefreshTokenHash(hasher.hmacSha256(newRefresh));
        next.setRefreshTokenEnc(crypto.encrypt(newRefresh));

        // Marca la anterior como revocada y encadenada
        cur.setRevokedAt(now);
        cur.setRotatedToSessionId(newSid);

        sessions.save(cur);
        sessions.save(next);

        return new RefreshedSession(newSid, sessionProps.getTtlSeconds());
    }

    public void logout(UUID sessionId) {
        Session cur = sessions.findById(sessionId)
                .orElseThrow(() -> new IllegalStateException("Session not found"));

        Instant now = Instant.now();
        if (cur.getRevokedAt() != null) return;

        // Revocar en OP (opcional)
        String refreshToken = crypto.decrypt(cur.getRefreshTokenEnc());
        idp.revokeRefreshToken(refreshToken);

        cur.setRevokedAt(now);
        sessions.save(cur);
    }
}