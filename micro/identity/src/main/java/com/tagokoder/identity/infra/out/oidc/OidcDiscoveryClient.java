package com.tagokoder.identity.infra.out.oidc;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import com.tagokoder.identity.application.OidcProperties;

import reactor.util.retry.Retry;

public class OidcDiscoveryClient {

  private static final Logger log = LoggerFactory.getLogger(OidcDiscoveryClient.class);

  private final WebClient webClient;
  private final OidcProperties props;

  // cache en memoria (no consultes discovery en cada request)
  private final AtomicReference<ProviderMetadata> cache = new AtomicReference<>();

  public OidcDiscoveryClient(WebClient oidcWebClient, OidcProperties props) {
    this.webClient = oidcWebClient;
    this.props = props;
  }

  public ProviderMetadata getMetadata() {
    ProviderMetadata cached = cache.get();
    if (cached != null) return cached;

    String issuer = props.getIssuer();
    if (issuer == null || issuer.isBlank()) {
      throw new IllegalStateException("oidc.issuer is required");
    }
    issuer = issuer.replaceAll("/+$", "");

    String wellKnown = issuer + "/.well-known/openid-configuration";

    // Subimos timeout (3s es muy poco). Puedes mover esto a props/env si quieres.
    int timeoutMs = 15000;

    boolean canFallbackStatic =
        notBlank(props.getAuthUrl()) &&
        notBlank(props.getTokenUrl()) &&
        notBlank(props.getUserInfoUrl());

    log.info("OIDC_DISCOVERY start url={} timeoutMs={} fallbackStatic={}", wellKnown, timeoutMs, canFallbackStatic);

    try {
      OpenIdConfiguration cfg = webClient.get()
          .uri(wellKnown)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .bodyToMono(OpenIdConfiguration.class)
          .timeout(Duration.ofMillis(timeoutMs))
          .retryWhen(
              Retry.backoff(2, Duration.ofMillis(250))
                  .maxBackoff(Duration.ofSeconds(2))
          )
          .doOnError(e -> log.error("OIDC_DISCOVERY failed url={} err={}", wellKnown, e.toString()))
          .block();

      if (cfg == null || cfg.issuer() == null || cfg.authorization_endpoint() == null || cfg.token_endpoint() == null) {
        throw new IllegalStateException("OIDC discovery returned incomplete metadata");
      }

      ProviderMetadata meta = new ProviderMetadata(
          nvl(cfg.issuer(), issuer),
          nvl(cfg.authorization_endpoint(), props.getAuthUrl()),
          nvl(cfg.token_endpoint(), props.getTokenUrl()),
          nvl(cfg.userinfo_endpoint(), props.getUserInfoUrl()),
          nvl(cfg.jwks_uri(), issuer + "/.well-known/jwks.json"),
          nvl(cfg.revocation_endpoint(), props.getRevocationUrl())
      );

      cache.set(meta);
      log.info("OIDC_DISCOVERY ok issuer={} auth={}", meta.issuer(), meta.authorization_endpoint());
      return meta;

    } catch (Exception e) {
      log.error("OIDC_DISCOVERY exception: {}", e.toString(), e);

      // fallback a tu config estática (ProviderMetadata.fromStatic)
      if (canFallbackStatic) {
        ProviderMetadata meta = ProviderMetadata.fromStatic(props);
        cache.set(meta);
        log.warn("OIDC_DISCOVERY fallback STATIC issuer={} auth={}", meta.issuer(), meta.authorization_endpoint());
        return meta;
      }

      // si no hay fallback posible, reventamos con mensaje claro
      throw new IllegalStateException("OIDC discovery failed and static fallback is not configured. url=" + wellKnown, e);
    }
  }

  // DTO para el JSON de /.well-known/openid-configuration
  public record OpenIdConfiguration(
      String issuer,
      String authorization_endpoint,
      String token_endpoint,
      String userinfo_endpoint,
      String jwks_uri,
      String revocation_endpoint
  ) {}

  private static boolean notBlank(String s) {
    return s != null && !s.isBlank();
  }

  private static String nvl(String v, String def) {
    return (v == null || v.isBlank()) ? def : v;
  }
}