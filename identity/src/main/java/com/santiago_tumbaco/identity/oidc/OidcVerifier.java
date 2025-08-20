package com.santiago_tumbaco.identity.oidc;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Component
public class OidcVerifier {
  private final JwtDecoder decoder;

  public OidcVerifier(@Value("${authentik.jwks}") String jwks,
                      @Value("${authentik.issuer}") String issuer) {
    NimbusJwtDecoder d = NimbusJwtDecoder.withJwkSetUri(jwks).build();
    d.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
    this.decoder = d;
  }
  public Jwt verify(String rawJwt) {
    if (rawJwt == null || rawJwt.isBlank()) throw new BadCredentialsException("missing token");
    return decoder.decode(rawJwt);
  }
}
