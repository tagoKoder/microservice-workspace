package com.tagokoder.account.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@Profile("!local")
public class JwtConfig {

    @Bean
    JwtDecoder jwtDecoder(AppProps props) {

        String issuer = props.security().issuerUri();
        String expectedClientId = props.security().audience(); // tu App Client ID

        if (issuer == null || issuer.isBlank()) {
        throw new IllegalStateException("Missing app.security.issuer-uri (COGNITO_ISSUER_URI)");
        }
        if (expectedClientId == null || expectedClientId.isBlank()) {
        throw new IllegalStateException("Missing app.security.audience (COGNITO_AUDIENCE)");
        }

        // Importante: issuer sin trailing slash para matchear iss del token
        issuer = issuer.replaceAll("/+$", "");

        String jwks = issuer + "/.well-known/jwks.json";
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);

        OAuth2TokenValidator<Jwt> tokenUseAccess = jwt -> {
        String tu = jwt.getClaimAsString("token_use");
        if ("access".equals(tu)) return OAuth2TokenValidatorResult.success();
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error("invalid_token", "token_use must be access", null)
        );
        };

        OAuth2TokenValidator<Jwt> clientIdOrAud = jwt -> {
        // Cognito access_token suele traer client_id (y a veces no trae aud)
        String clientId = jwt.getClaimAsString("client_id");
        if (expectedClientId.equals(clientId)) {
            return OAuth2TokenValidatorResult.success();
        }
        if (jwt.getAudience() != null && jwt.getAudience().contains(expectedClientId)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error("invalid_token", "client_id/aud mismatch", null)
        );
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, tokenUseAccess, clientIdOrAud));
        return decoder;
    }
}
