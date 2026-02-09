package com.tagokoder.identity.infra.out.oidc;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.tagokoder.identity.application.OidcProperties;

@Component
public class OidcDiscoveryClient {

    private final WebClient web;
    private final OidcProperties props;

    private final AtomicReference<Cached> cache = new AtomicReference<>();

    public OidcDiscoveryClient(WebClient.Builder builder, OidcProperties props) {
        this.web = builder.build();
        this.props = props;
    }

    public ProviderMetadata getMetadata() {
        if (!props.isDiscoveryEnabled()) {
            return ProviderMetadata.fromStatic(props);
        }

        Cached c = cache.get();
        long now = System.currentTimeMillis();
        if (c != null && (now - c.cachedAtMs) < Duration.ofMinutes(10).toMillis()) {
            return c.meta;
        }

        String issuerStr = normalizeIssuer(props.getIssuer());
        URI issuer = URI.create(issuerStr);
        URI wellKnown = issuer.resolve(".well-known/openid-configuration");

        ProviderMetadata meta = web.get()
                .uri(wellKnown)
                .retrieve()
                .bodyToMono(ProviderMetadata.class)
                .timeout(Duration.ofSeconds(3))
                .block();

        if (meta == null || meta.issuer() == null || meta.jwks_uri() == null) {
            throw new IllegalStateException("OIDC discovery returned invalid metadata");
        }

        if (!normalizeIssuer(meta.issuer()).equals(normalizeIssuer(props.getIssuer()))) {
            throw new IllegalStateException("OIDC issuer mismatch (possible mix-up attack)");
        }

        cache.set(new Cached(meta, now));
        return meta;
    }



    private String normalizeIssuer(String issuer) {
        if (issuer == null) return "";
        return issuer.endsWith("/") ? issuer : issuer + "/";
    }

    

    private record Cached(ProviderMetadata meta, long cachedAtMs) {}
}
