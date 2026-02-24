package com.tagokoder.identity.application.service;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tagokoder.identity.application.AppProps;
import com.tagokoder.identity.application.OidcProperties;
import com.tagokoder.identity.domain.model.Identity;
import com.tagokoder.identity.domain.port.in.CompleteLoginUseCase;
import com.tagokoder.identity.domain.port.in.CreateSessionUseCase;
import com.tagokoder.identity.domain.port.in.StartLoginUseCase;
import com.tagokoder.identity.domain.port.out.AuditPublisher;
import com.tagokoder.identity.domain.port.out.IdentityLinkRepositoryPort;
import com.tagokoder.identity.domain.port.out.IdentityRepositoryPort;
import com.tagokoder.identity.domain.port.out.OidcIdpClientPort;
import com.tagokoder.identity.domain.port.out.OidcStateRepositoryPort;
import com.tagokoder.identity.domain.port.out.RegistrationIntentRepositoryPort;
import com.tagokoder.identity.infra.audit.AuditEventV1;
import com.tagokoder.identity.infra.security.OidcIdTokenValidator;
import com.tagokoder.identity.infra.security.grpc.CorrelationServerInterceptor;

import jakarta.transaction.Transactional;
@Service
public class OidcAuthService implements StartLoginUseCase, CompleteLoginUseCase {

    private final OidcStateRepositoryPort stateRepo;
    private final OidcIdpClientPort idpClient;
    private final IdentityRepositoryPort identityRepo;
    private final OidcProperties properties;
    private final OidcIdTokenValidator idTokenValidator;
    private final CreateSessionUseCase createSessionUseCase;
    private final AuditPublisher audit;
    private final AppProps props;
    private final RegistrationIntentRepositoryPort registrationRepo;
    private final IdentityLinkRepositoryPort identityLinks;



    private final SecureRandom random = new SecureRandom();

    public OidcAuthService(OidcStateRepositoryPort stateRepo,
                           OidcIdpClientPort idpClient,
                           IdentityRepositoryPort identityRepo,
                           OidcProperties properties,
                           OidcIdTokenValidator idTokenValidator,
                           CreateSessionUseCase createSessionUseCase,
                           AuditPublisher audit, AppProps props,
                           RegistrationIntentRepositoryPort registrationRepo,
                           IdentityLinkRepositoryPort identityLinks) {
        this.stateRepo = stateRepo;
        this.idpClient = idpClient;
        this.identityRepo = identityRepo;
        this.properties = properties;
        this.idTokenValidator = idTokenValidator;
        this.createSessionUseCase = createSessionUseCase;
        this.audit = audit;
        this.props = props;
        this.registrationRepo = registrationRepo;
        this.identityLinks = identityLinks;
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
        var cid = CorrelationServerInterceptor.getCorrelationId();

        audit.publish(new AuditEventV1(
        "1.0",
        Instant.now(),
        "identity",
        props.env(),
        cid,
        "grpc:/bank.identity.v1.OidcAuthService/StartOidcLogin",
        "identity.oidc.start_login",
        200,
        new AuditEventV1.Actor(null, properties.getProvider()),
        Map.of("channel", command.channel()),
        Map.of(),
        null
        ));

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
        linkIdentityToCustomerIfPossible(identity.getId(), email);
        // 3) Sesión server-side: guardas refresh token cifrado + hash
        var created = createSessionUseCase.createSession(
            identity, 
            token.refreshToken, 
            token.accessToken,
            token.expiresIn,
            command.ip(), 
            command.userAgent());

            // --- AUDIT: session created (best-effort) ---
        try {
            var cid = CorrelationServerInterceptor.getCorrelationId();

            audit.publish(new AuditEventV1(
                "1.0",
                Instant.now(),
                "identity",
                props.env(),
                cid,
                "grpc:/bank.identity.v1.OidcAuthService/CompleteOidcLogin",
                "identity.session.created",
                200,
                new AuditEventV1.Actor(subject, provider),
                Map.of(
                    "flow", "oidc",
                    "session_id", created.sessionId().toString(),   // OK (no es PII)
                    "session_ttl_sec", created.expiresInSeconds()
                ),
                Map.of(),
                null
            ));
        } catch (Exception ignore) {
            // no rompas el login por auditoría
        }


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

    @Transactional
    private void linkIdentityToCustomerIfPossible(UUID identityId, String email) {
    if (email == null || email.isBlank()) return;

    var regOpt = registrationRepo.findLatestByEmail(email); // crear este método
    if (regOpt.isEmpty()) return;

    var reg = regOpt.get();

    // 1) persistir identityId dentro del registro (para el caso login->activate)
    if (reg.getIdentityId() == null) {
        reg.setIdentityId(identityId);
        registrationRepo.save(reg);
    }

    // 2) si ya existe customerId, link inmediato
    if (reg.getCustomerId() != null && !reg.getCustomerId().isBlank()) {
        identityLinks.upsert(identityId, reg.getCustomerId());
    }
    }

}