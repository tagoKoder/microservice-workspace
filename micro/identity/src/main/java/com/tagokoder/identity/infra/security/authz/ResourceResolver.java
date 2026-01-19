package com.tagokoder.identity.infra.security.authz;

import java.util.Map;
import java.util.UUID;

public class ResourceResolver {

    public record ResourceDef(String type, String id, Map<String, Object> attrs) {}

    public ResourceDef resolve(String fullMethodName, Object request, String principalCustomerIdOrNull) {

        UUID regId = extractUuid(request, "getRegistrationId");
        if (regId != null) {
            return new ResourceDef("Registration", regId.toString(), Map.of());
        }

        UUID sessionId = extractUuid(request, "getSessionId");
        if (sessionId != null) {
            return new ResourceDef("Session", sessionId.toString(), Map.of());
        }

        // OIDC start: no hay resource id, usar System
        return new ResourceDef("System", "system", Map.of());
    }

    private UUID extractUuid(Object request, String getter) {
        try {
            var m = request.getClass().getMethod(getter);
            Object v = m.invoke(request);
            if (v instanceof String s && !s.isBlank()) return UUID.fromString(s);
        } catch (Exception ignored) {}
        return null;
    }
}
