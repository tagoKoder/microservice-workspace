package com.tagokoder.identity.infra.security;

import java.util.List;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.tagokoder.identity.application.OidcProperties;
import com.tagokoder.identity.infra.out.oidc.OidcDiscoveryClient;

@Component
public class OidcIdTokenValidator {

    private final OidcDiscoveryClient discovery;
    private final OidcProperties props;

    public OidcIdTokenValidator(OidcDiscoveryClient discovery, OidcProperties props) {
        this.discovery = discovery;
        this.props = props;
    }

    public Jwt validate(String idToken, String expectedNonce) {
        var meta = discovery.getMetadata();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(meta.jwks_uri()).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(meta.issuer());

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            var aud = jwt.getAudience();
            if (aud != null && aud.contains(props.getClientId())) return OAuth2TokenValidatorResult.success();
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "aud mismatch", null));
        };

        // Cognito: token_use debe ser "id"
        OAuth2TokenValidator<Jwt> tokenUseValidator = jwt -> {
            String tokenUse = jwt.getClaimAsString("token_use");
            if (tokenUse == null || !"id".equals(tokenUse)) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "token_use must be id", null));
            }
            return OAuth2TokenValidatorResult.success();
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator, tokenUseValidator));

        Jwt jwt = decoder.decode(idToken);

        String nonce = jwt.getClaimAsString("nonce");
        if (nonce == null || expectedNonce == null || !nonce.equals(expectedNonce)) {
            throw new JwtValidationException("nonce mismatch",
                    List.of(new OAuth2Error("invalid_token", "nonce mismatch", null)));
        }
        return jwt;
    }
}
