package com.tagokoder.identity.application;

import lombok.Data;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identity.oidc")
@Data
public class OidcProperties {
    private String bffCallbackUrl;
    private String provider; // "cognito"

    private String issuer;
    private boolean discoveryEnabled = true;

    // overrides opcionales
    private String authUrl;

    private String tokenUrl;
    private String userInfoUrl;
    private String revocationUrl;

    private String clientId;
    private String clientSecret;

    // recomendado: controlado por config
    private String scope = "openid profile email";

    /**
     * Prefijos permitidos para redirect_after_login (previene open-redirect).
     * Ejemplo: ["/", "/app", "/dashboard"]
     */
    private List<String> allowedRedirectPrefixes = List.of("/");

    public String sanitizeRedirectAfterLogin(String raw) {
        if (raw == null || raw.isBlank()) return "/";

        String r = raw.trim();

        // bloquea esquemas y protocol-relative
        if (r.startsWith("http://") || r.startsWith("https://") || r.startsWith("//")) return "/";
        if (r.contains("://")) return "/";

        // solo paths internos
        if (!r.startsWith("/")) return "/";
        if (r.contains("\\") || r.contains("\r") || r.contains("\n")) return "/";

        List<String> allowed = getAllowedRedirectPrefixes(); // Lombok genera el getter
        if (allowed == null || allowed.isEmpty()) return "/";

        for (String p : allowed) {
            if (p != null && !p.isBlank() && r.startsWith(p)) return r;
        }
        return "/";
    }
}

