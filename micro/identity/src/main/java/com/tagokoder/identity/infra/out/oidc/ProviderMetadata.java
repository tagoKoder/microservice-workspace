package com.tagokoder.identity.infra.out.oidc;

import com.tagokoder.identity.application.OidcProperties;

public record ProviderMetadata(
        String issuer,
        String authorization_endpoint,
        String token_endpoint,
        String userinfo_endpoint,
        String jwks_uri,
        String revocation_endpoint
) {
    public static ProviderMetadata fromStatic(OidcProperties p) {
        String issuer = p.getIssuer().replaceAll("/+$", "");
        String jwks = issuer + "/.well-known/jwks.json";
        return new ProviderMetadata(
                issuer,
                p.getAuthUrl(),
                p.getTokenUrl(),
                p.getUserInfoUrl(),
                jwks,
                p.getRevocationUrl()
        );
    }


    
}
