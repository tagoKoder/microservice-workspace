package com.tagokoder.identity.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.tagokoder.identity.application.AppProps;

@Configuration
public class JwtDecoderConfig {

    @Bean
    @Profile("!local")
    public JwtDecoder jwtDecoder(AppProps props) {
        String issuer = props.security().issuerUri();
        String audience = props.security().audience(); // Cognito App Client ID

        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("app.security.issuerUri (COGNITO_ISSUER) es requerido en perfiles no-local");
        }
        if (audience == null || audience.isBlank()) {
            throw new IllegalStateException("app.security.audience (COGNITO_CLIENT_ID) es requerido en perfiles no-local");
        }

        // IMPORTANT: issuer sin trailing slash (como en account)
        issuer = issuer.replaceAll("/+$", "");

        JwtDecoder base = JwtDecoders.fromIssuerLocation(issuer);
        if (!(base instanceof NimbusJwtDecoder decoder)) {
            throw new IllegalStateException("Expected NimbusJwtDecoder from issuer=" + issuer);
        }

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);

        OAuth2TokenValidator<Jwt> tokenUseAccess = jwt -> {
            String tu = jwt.getClaimAsString("token_use");
            if ("access".equals(tu)) return OAuth2TokenValidatorResult.success();
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "token_use must be access", null)
            );
        };

        OAuth2TokenValidator<Jwt> clientIdOrAudValidator = jwt -> {
            // Cognito access token: client_id suele estar; aud a veces no
            String clientId = jwt.getClaimAsString("client_id");
            if (audience.equals(clientId)) return OAuth2TokenValidatorResult.success();

            var aud = jwt.getAudience();
            if (aud != null && aud.contains(audience)) return OAuth2TokenValidatorResult.success();

            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "client_id/aud mismatch", null)
            );
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, tokenUseAccess, clientIdOrAudValidator));
        return decoder;
    }
}
