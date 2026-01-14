package com.tagokoder.identity.infra.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class TokenCrypto {

    private final byte[] key; // 32 bytes
    private final SecureRandom random = new SecureRandom();

    public TokenCrypto(String keyB64) {
        if (keyB64 == null || keyB64.isBlank()) {
            throw new IllegalStateException("IDENTITY_REFRESH_TOKEN_ENC_KEY_B64 is required");
        }
        this.key = Base64.getDecoder().decode(keyB64);
        if (this.key.length != 32) {
            throw new IllegalStateException("refreshTokenEncKeyB64 must be 32 bytes base64");
        }
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);

            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot encrypt token", e);
        }
    }

    public String decrypt(String payloadB64) {
        try {
            byte[] payload = Base64.getDecoder().decode(payloadB64);
            byte[] iv = new byte[12];
            byte[] ct = new byte[payload.length - 12];

            System.arraycopy(payload, 0, iv, 0, 12);
            System.arraycopy(payload, 12, ct, 0, ct.length);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] pt = c.doFinal(ct);
            return new String(pt, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot decrypt token", e);
        }
    }
}
