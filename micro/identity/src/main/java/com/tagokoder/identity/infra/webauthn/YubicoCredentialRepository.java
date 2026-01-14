package com.tagokoder.identity.infra.webauthn;

import java.util.*;
import java.util.stream.Collectors;

import com.tagokoder.identity.domain.port.out.WebauthnCredentialRepositoryPort;
import com.tagokoder.identity.domain.port.out.WebauthnCredentialRepositoryPort.WebauthnCredential;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialType;

public class YubicoCredentialRepository implements CredentialRepository {

    private final WebauthnCredentialRepositoryPort repo;

    public YubicoCredentialRepository(WebauthnCredentialRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        UUID identityId = UUID.fromString(username);
        List<WebauthnCredential> creds = repo.findByIdentityId(identityId).stream()
            .filter(WebauthnCredential::enabled)
            .toList();

        return creds.stream()
            .map(c -> PublicKeyCredentialDescriptor.builder()
                .id(fromBase64UrlUnsafe(c.credentialId()))
                .type(PublicKeyCredentialType.PUBLIC_KEY)
                .build())
            .collect(Collectors.toSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        UUID id = UUID.fromString(username);
        return Optional.of(uuidToByteArray(id));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        UUID id = byteArrayToUuid(userHandle);
        return Optional.of(id.toString());
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        UUID identityId = byteArrayToUuid(userHandle);
        String credId = credentialId.getBase64Url();

        return repo.findByCredentialId(credId)
            .filter(WebauthnCredential::enabled)
            .filter(c -> c.identityId().equals(identityId))
            .map(c -> RegisteredCredential.builder()
                .credentialId(fromBase64UrlUnsafe(c.credentialId()))
                .userHandle(uuidToByteArray(c.identityId()))
                .publicKeyCose(new ByteArray(c.publicKeyCose()))
                .signatureCount(c.signCount())
                .build());
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        String credId = credentialId.getBase64Url();
        return repo.findByCredentialId(credId)
            .filter(WebauthnCredential::enabled)
            .map(c -> RegisteredCredential.builder()
                .credentialId(fromBase64UrlUnsafe(c.credentialId()))
                .userHandle(uuidToByteArray(c.identityId()))
                .publicKeyCose(new ByteArray(c.publicKeyCose()))
                .signatureCount(c.signCount())
                .build())
            .map(Set::of)
            .orElseGet(Set::of);
    }

    private static ByteArray fromBase64UrlUnsafe(String b64u) {
        try {
            return ByteArray.fromBase64Url(b64u);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid base64url credentialId", e);
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
