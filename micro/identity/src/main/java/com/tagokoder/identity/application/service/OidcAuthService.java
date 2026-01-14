package com.tagokoder.identity.application.service;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tagokoder.identity.application.OidcProperties;
import com.tagokoder.identity.domain.model.Identity;
import com.tagokoder.identity.domain.port.in.CompleteLoginUseCase;
import com.tagokoder.identity.domain.port.in.CreateSessionUseCase;
import com.tagokoder.identity.domain.port.in.StartLoginUseCase;
import com.tagokoder.identity.domain.port.out.IdentityRepositoryPort;
import com.tagokoder.identity.domain.port.out.OidcIdpClientPort;
import com.tagokoder.identity.domain.port.out.OidcStateRepositoryPort;
import com.tagokoder.identity.domain.port.out.WebauthnCredentialRepositoryPort;
import com.tagokoder.identity.infra.security.OidcIdTokenValidator;

@Service
public class OidcAuthService implements StartLoginUseCase, CompleteLoginUseCase {

    private final OidcStateRepositoryPort stateRepo;
    private final OidcIdpClientPort idpClient;
    private final IdentityRepositoryPort identityRepo;
    private final OidcProperties properties;
    private final OidcIdTokenValidator idTokenValidator;
    private final CreateSessionUseCase createSessionUseCase;
    private final WebauthnCredentialRepositoryPort creds;


    private final SecureRandom random = new SecureRandom();

    public OidcAuthService(OidcStateRepositoryPort stateRepo,
                           OidcIdpClientPort idpClient,
                           IdentityRepositoryPort identityRepo,
                           OidcProperties properties,
                           OidcIdTokenValidator idTokenValidator,
                           CreateSessionUseCase createSessionUseCase,
                           WebauthnCredentialRepositoryPort creds) {
        this.stateRepo = stateRepo;
        this.idpClient = idpClient;
        this.identityRepo = identityRepo;
        this.properties = properties;
        this.idTokenValidator = idTokenValidator;
        this.createSessionUseCase = createSessionUseCase;
        this.creds = creds;
    }

    @Override
    public StartLoginResponse start(StartLoginCommand command) {
        String state = randomUrlSafe(16);
        String codeVerifier = randomUrlSafe(32);
        String codeChallenge = pkceS256(codeVerifier);

        String redirectUri = properties.getBffCallbackUrl();

        String nonce = randomUrlSafe(16);

        stateRepo.saveState(
            state,
            codeVerifier,
            properties.sanitizeRedirectAfterLogin(command.redirectAfterLogin()),
            nonce,
            Duration.ofMinutes(10)
        );

        String authUrl = idpClient.buildAuthorizationUrl(state, nonce, codeChallenge, redirectUri);

        return new StartLoginResponse(authUrl, state);
    }

    @Override
    public CompleteLoginResponse complete(CompleteLoginCommand command) {
        var oidcState = stateRepo.loadAndRemove(command.state());
        if (oidcState == null) throw new IllegalStateException("Invalid or expired state");

        String redirectUri = properties.getBffCallbackUrl();

        var token = idpClient.exchangeCodeForTokens(command.code(), oidcState.codeVerifier(), redirectUri);

        // 1) Validar ID token + nonce
        var idJwt = idTokenValidator.validate(token.idToken, oidcState.nonce());

        // 2) Extraer identidad desde el token (preferente)
        String subject = idJwt.getSubject();
        String email = idJwt.getClaimAsString("email");
        String name  = idJwt.getClaimAsString("name");

        // Grupos/roles
        var groups = idJwt.getClaimAsStringList("cognito:groups");
        if (groups == null) groups = idJwt.getClaimAsStringList("groups");
        if (groups == null) groups = java.util.List.of();

        // Fallback (si faltan campos) usando userInfo
        if (email == null || name == null) {
            var userInfo = idpClient.fetchUserInfo(token.accessToken);
            if (email == null) email = userInfo.email;
            if (name == null)  name  = userInfo.name;
        }

        String provider = properties.getProvider(); // "cognito"

        Identity identity = identityRepo
                .findBySubjectAndProvider(subject, provider)
                .orElseGet(() -> new Identity(
                        UUID.randomUUID(),
                        subject,
                        provider,
                        Identity.UserStatus.ACTIVE,
                        Instant.now()
                ));

        if (!identity.isActive()) identity = identity.activate();
        identity = identityRepo.save(identity);
        boolean mfaRequired = creds.countEnabledByIdentityId(identity.getId()) > 0;
        // 3) Sesión server-side: guardas refresh token cifrado + hash
        var created = createSessionUseCase.createSession(identity, token.refreshToken, command.ip(), command.userAgent(), mfaRequired);

        return new CompleteLoginResponse(
                identity.getId(),
                subject,
                provider,
                name,
                email,
                groups,
                created.sessionId(),
                created.expiresInSeconds(),
                oidcState.redirectAfterLogin()
        );
    }


    // helpers

    private String randomUrlSafe(int bytes) {
        byte[] buf = new byte[bytes];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private String pkceS256(String verifier) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(verifier.getBytes(UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot compute PKCE S256", e);
        }
    }
}