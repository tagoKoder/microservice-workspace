package com.tagokoder.account.infra.security.authz;

public record RouteDef(
    String actionId,
    boolean critical,
    AuthzMode mode,
    boolean requireCustomerLink,
    String resourceTemplate
) {}