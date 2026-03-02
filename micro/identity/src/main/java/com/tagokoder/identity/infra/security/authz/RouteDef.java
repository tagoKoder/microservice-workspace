package com.tagokoder.identity.infra.security.authz;

public record RouteDef(
    String actionId,
    boolean critical,
    AuthzMode mode,
    String resourceTemplate
) {}