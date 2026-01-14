package com.tagokoder.identity.infra.security;

import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class TokenHasher {

    private final byte[] pepper;

    public TokenHasher(String pepper) {
        if (pepper == null || pepper.isBlank()) {
            throw new IllegalStateException("IDENTITY_REFRESH_TOKEN_PEPPER is required");
        }
        this.pepper = pepper.getBytes(StandardCharsets.UTF_8);
    }

    public String hmacSha256(String token) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            byte[] out = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot HMAC token", e);
        }
    }

    // comparación constante (evita timing leaks)
    public boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
