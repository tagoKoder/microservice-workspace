package com.tagokoder.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "identity.clients")
@Data
public class IdentityClientsProperties {
    private String accountsTarget; // e.g. "dns:///accounts:9090"
    private String ledgerTarget;   // e.g. "dns:///ledgerpayments:9090"

    // Como /onboarding/activate es público en tu OpenAPI, Identity no siempre tendrá JWT de usuario.
    // Para no romper ASVS, usa un token de servicio (Cognito) con permisos mínimos.
    private String serviceAccessToken; // JWT (rotar en prod)
}
