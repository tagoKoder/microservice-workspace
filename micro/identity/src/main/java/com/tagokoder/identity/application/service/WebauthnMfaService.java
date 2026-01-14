package com.tagokoder.identity.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagokoder.identity.domain.port.in.WebauthnMfaUseCase;
import com.tagokoder.identity.domain.port.out.SessionRepositoryPort;
import com.tagokoder.identity.domain.port.out.WebauthnChallengeStorePort;
import com.tagokoder.identity.domain.port.out.WebauthnCredentialRepositoryPort;
import com.tagokoder.identity.domain.port.out.WebauthnCredentialRepositoryPort.WebauthnCredential;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.UserIdentity;

@Service
public class WebauthnMfaService implements WebauthnMfaUseCase {

    private final RelyingParty rp;
    private final WebauthnCredentialRepositoryPort creds;
    private final WebauthnChallengeStorePort challenges;
    private final SessionRepositoryPort sessions;
    private final ObjectMapper om;

    public WebauthnMfaService(RelyingParty rp,
                             WebauthnCredentialRepositoryPort creds,
                             WebauthnChallengeStorePort challenges,
                             SessionRepositoryPort sessions,
                             ObjectMapper om) {
        this.rp = rp;
        this.creds = creds;
        this.challenges = challenges;
        this.sessions = sessions;
        this.om = om;
    }

    @Override
    public BeginRegistrationResponse beginRegistration(BeginRegistrationCommand c) {
        String requestId = UUID.randomUUID().toString();

        // username = identityId
        UserIdentity user = UserIdentity.builder()
            .name(c.identityId().toString())
            .displayName(c.identityId().toString())
            .id(uuidToByteArray(c.identityId()))
            .build();

        PublicKeyCredentialCreationOptions options = rp.startRegistration(
            com.yubico.webauthn.StartRegistrationOptions.builder()
                .user(user)
                .timeout(Optional.of(30000L))
                .build()
        );

        String json = toJson(options);
        challenges.save(requestId, json, Duration.ofMinutes(5));

        return new BeginRegistrationResponse(requestId, json);
    }

    @Override
    @Transactional
    public void finishRegistration(FinishRegistrationCommand c) {
        String reqJson = challenges.loadAndRemove(c.requestId());
        if (reqJson == null) throw new IllegalStateException("Invalid/expired requestId");

        try {
            PublicKeyCredentialCreationOptions request =
                PublicKeyCredentialCreationOptions.fromJson(reqJson);

            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> response =
                PublicKeyCredential.parseRegistrationResponseJson(c.credentialJson());

            var result = rp.finishRegistration(
                com.yubico.webauthn.FinishRegistrationOptions.builder()
                    .request(request)
                    .response(response)
                    .build()
            );

            UUID identityId = byteArrayToUuid(request.getUser().getId());

            WebauthnCredential record = new WebauthnCredential(
                UUID.randomUUID(),
                identityId,
                result.getKeyId().getId().getBase64Url(),
                result.getPublicKeyCose().getBytes(),
                result.getSignatureCount(),
                null,   // aaguid: lo dejamos null para evitar incompatibilidades de versión
                null,   // transports opcional
                null,   // name opcional (puedes mapear c.deviceName() si lo quieres persistir)
                true,
                Instant.now(),
                null
            );

            creds.save(record);

        } catch (Exception e) {
            throw new IllegalStateException("WebAuthn registration failed", e);
        }
    }

    @Override
    public BeginAssertionResponse beginAssertion(BeginAssertionCommand c) {
        String requestId = UUID.randomUUID().toString();

        // username = identityId
        AssertionRequest req = rp.startAssertion(
            com.yubico.webauthn.StartAssertionOptions.builder()
                .username(Optional.of(c.identityId().toString()))
                .timeout(Optional.of(30000L))
                .build()
        );

        String json = toJson(req);
        challenges.save(requestId, json, Duration.ofMinutes(5));

        return new BeginAssertionResponse(requestId, json);
    }

    @Override
    @Transactional
    public void finishAssertion(FinishAssertionCommand c) {
        String reqJson = challenges.loadAndRemove(c.requestId());
        if (reqJson == null) throw new IllegalStateException("Invalid/expired requestId");

        try {
            AssertionRequest request = om.readValue(reqJson, AssertionRequest.class);

            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> response =
                PublicKeyCredential.parseAssertionResponseJson(c.credentialJson());

            var result = rp.finishAssertion(
                com.yubico.webauthn.FinishAssertionOptions.builder()
                    .request(request)
                    .response(response)
                    .build()
            );

            if (!result.isSuccess()) {
                throw new IllegalStateException("WebAuthn assertion not successful");
            }

            // actualiza signCount en DB si hace falta
            var credId = response.getId().getBase64Url();
            var domainCred = creds.findByCredentialId(credId).orElseThrow();

            creds.updateSignCountAndLastUsed(domainCred.id(), result.getSignatureCount(), Instant.now());

            // marca la sesión como MFA verificada
            var sess = sessions.findById(c.sessionId()).orElseThrow();
            sess.setMfaVerifiedAt(Instant.now());
            sessions.save(sess);

        } catch (Exception e) {
            throw new IllegalStateException("WebAuthn assertion failed", e);
        }
    }

    private String toJson(Object o) {
        try {
            return om.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize WebAuthn object", e);
        }
    }

    private static ByteArray uuidToByteArray(UUID id) {
        byte[] b = new byte[16];
        var bb = java.nio.ByteBuffer.wrap(b);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        return new ByteArray(b);
    }

    private static UUID byteArrayToUuid(ByteArray ba) {
        byte[] b = ba.getBytes();
        var bb = java.nio.ByteBuffer.wrap(b);
        long msb = bb.getLong();
        long lsb = bb.getLong();
        return new UUID(msb, lsb);
    }
}
