package com.tagokoder.identity.application.service;


import com.tagokoder.identity.application.OidcProperties;
import com.tagokoder.identity.domain.model.Identity;
import com.tagokoder.identity.domain.port.in.CompleteLoginUseCase;
import com.tagokoder.identity.domain.port.in.StartLoginUseCase;
import com.tagokoder.identity.domain.port.out.IdentityRepositoryPort;
import com.tagokoder.identity.domain.port.out.OidcIdpClientPort;
import com.tagokoder.identity.domain.port.out.OidcStateRepositoryPort;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class OidcAuthService implements StartLoginUseCase, CompleteLoginUseCase {

    private final OidcStateRepositoryPort stateRepo;
    private final OidcIdpClientPort idpClient;
    private final IdentityRepositoryPort identityRepo;
    private final OidcProperties properties;

    private final SecureRandom random = new SecureRandom();

    public OidcAuthService(OidcStateRepositoryPort stateRepo,
                           OidcIdpClientPort idpClient,
                           IdentityRepositoryPort identityRepo,
                           OidcProperties properties) {
        this.stateRepo = stateRepo;
        this.idpClient = idpClient;
        this.identityRepo = identityRepo;
        this.properties = properties;
    }

    @Override
    public StartLoginResponse start(StartLoginCommand command) {
        String state = randomUrlSafe(16);
        String codeVerifier = randomUrlSafe(32);
        String codeChallenge = pkceS256(codeVerifier);

        String redirectUri = properties.getBffCallbackUrl();

        stateRepo.saveState(
                state,
                codeVerifier,
                command.redirectAfterLogin(),
                Duration.ofMinutes(10)
        );

        String authUrl = idpClient.buildAuthorizationUrl(state, codeChallenge, redirectUri);
        return new StartLoginResponse(authUrl, state);
    }

    @Override
    public CompleteLoginResponse complete(CompleteLoginCommand command) {
        var oidcState = stateRepo.loadAndRemove(command.state());
        if (oidcState == null) {
            throw new IllegalStateException("Invalid or expired state");
        }

        String redirectUri = properties.getBffCallbackUrl();
        var token = idpClient.exchangeCodeForTokens(command.code(), oidcState.codeVerifier, redirectUri);
        var userInfo = idpClient.fetchUserInfo(token.accessToken);

        String provider = properties.getProvider();
        Identity identity = identityRepo
                .findBySubjectAndProvider(userInfo.subject, provider)
                .orElseGet(() -> new Identity(
                        UUID.randomUUID(),
                        userInfo.subject,
                        provider,
                        Identity.UserStatus.ACTIVE,
                        Instant.now()
                ));

        if (!identity.isActive()) {
            identity = identity.activate();
        }

        identity = identityRepo.save(identity);

        return new CompleteLoginResponse(
                identity.getId(),
                userInfo.subject,
                provider,
                userInfo.name,
                userInfo.email,
                userInfo.groups,
                token.accessToken,
                token.refreshToken,
                token.expiresIn,
                oidcState.redirectAfterLogin
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