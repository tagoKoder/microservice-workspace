package com.santiago_tumbaco.identity.security;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MultiIssuerJwtVerifier {

  private final Set<String> allowedIssuers;
  private final Map<String, JwtDecoder> decoders = new ConcurrentHashMap<>();
  private final Map<String, List<String>> audiencesByIssuer;

  public MultiIssuerJwtVerifier(
      @Value("${idp.issuers}") String issuersCsv,
      @Value("#{${idp.audiences:{}}}") Map<String, List<String>> audiencesByIssuer // puede ser null
  ) {
    this.allowedIssuers = new HashSet<>();
    for (String iss : issuersCsv.split(",")) {
      String norm = iss.trim();
      if (!norm.isEmpty()) allowedIssuers.add(norm);
    }
    this.audiencesByIssuer = audiencesByIssuer == null ? Map.of() : audiencesByIssuer;
  }

  public Jwt verify(String rawJwt) {
    if (rawJwt == null || rawJwt.isBlank()) throw new BadCredentialsException("missing token");

    String iss = extractIssuerUntrusted(rawJwt);
    if (!allowedIssuers.contains(iss)) {
      throw new BadCredentialsException("untrusted issuer: " + iss);
    }

    JwtDecoder decoder = decoders.computeIfAbsent(iss, this::buildDecoderForIssuer);
    return decoder.decode(rawJwt);
  }

  private JwtDecoder buildDecoderForIssuer(String issuer) {
    // Descubrimiento OIDC -> jwks_uri → NimbusJwtDecoder
    NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuer);

    // Validador: estándar + issuer + (opcional) audience
    OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);

    List<String> auds = audiencesByIssuer.getOrDefault(issuer, List.of());
    OAuth2TokenValidator<Jwt> audienceVal = (jwt) -> {
      if (auds.isEmpty()) return OAuth2TokenValidatorResult.success();
      List<String> tokenAud = jwt.getAudience();
      boolean ok = tokenAud != null && tokenAud.stream().anyMatch(auds::contains);
      return ok ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "invalid audience", null));
    };

    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, audienceVal));
    return decoder;
  }

  private static String extractIssuerUntrusted(String raw) {
    try {
      return SignedJWT.parse(raw).getJWTClaimsSet().getIssuer();
    } catch (ParseException e) {
      throw new BadCredentialsException("invalid token");
    }
  }
}
