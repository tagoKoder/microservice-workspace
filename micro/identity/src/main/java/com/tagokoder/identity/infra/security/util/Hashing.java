package com.tagokoder.identity.infra.security.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Hashing {
    private Hashing() {}

    public static String sha256Hex(String salt, String value) {
        if (value == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest((salt + ":" + value).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
