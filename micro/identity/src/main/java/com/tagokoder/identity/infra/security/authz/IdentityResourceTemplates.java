package com.tagokoder.identity.infra.security.authz;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.tagokoder.identity.infra.security.avp.AvpValues;

import software.amazon.awssdk.services.verifiedpermissions.model.AttributeValue;
import software.amazon.awssdk.services.verifiedpermissions.model.EntityIdentifier;
import software.amazon.awssdk.services.verifiedpermissions.model.EntityItem;

public class IdentityResourceTemplates {

  public record Resolved(
      EntityItem resourceEntity,
      List<EntityItem> extraEntities,
      Map<String, AttributeValue> contextAttrs
  ) {}

  // ⚠️ Si aún no tienes estos entityTypes en tu schema, NO pongas rutas en AUTHZ todavía.
  // Puedes dejar todo en AUTHN_ONLY y ya. Esto es “future-proof”.
  private static final String SYSTEM_TYPE       = "ImaginaryBank::System";
  private static final String REGISTRATION_TYPE = "ImaginaryBank::Registration";
  private static final String SESSION_TYPE      = "ImaginaryBank::Session";

  public Resolved resolve(String template, String route, Object request) {
    return switch (template) {
      case RouteAuthzRegistry.T_SYSTEM -> system();
      case RouteAuthzRegistry.T_REGISTRATION_ID -> byUuid(request, "getRegistrationId", REGISTRATION_TYPE, "registration_id");
      case RouteAuthzRegistry.T_SESSION_ID -> byUuid(request, "getSessionId", SESSION_TYPE, "session_id");
      default -> throw new IllegalStateException("Unknown resource template: " + template + " route=" + route);
    };
  }

  private Resolved system() {
    String rid = "system";
    var attrs = new HashMap<String, AttributeValue>();
    attrs.put("id", AvpValues.str(rid));
    EntityIdentifier id = EntityIdentifier.builder().entityType(SYSTEM_TYPE).entityId(rid).build();
    EntityItem resource = EntityItem.builder().identifier(id).attributes(attrs).build();
    return new Resolved(resource, List.of(), Map.of());
  }

  private Resolved byUuid(Object request, String getter, String type, String attrName) {
    UUID idVal = extractUuidFromGetter(request, getter);
    String rid = (idVal == null) ? "unknown" : idVal.toString();

    var attrs = new HashMap<String, AttributeValue>();
    attrs.put(attrName, AvpValues.str(rid));

    EntityIdentifier id = EntityIdentifier.builder().entityType(type).entityId(rid).build();
    EntityItem resource = EntityItem.builder().identifier(id).attributes(attrs).build();
    return new Resolved(resource, List.of(), Map.of());
  }

  private static UUID extractUuidFromGetter(Object req, String getter) {
    try {
      var m = req.getClass().getMethod(getter);
      Object v = m.invoke(req);
      if (v instanceof String s && !s.isBlank()) return UUID.fromString(s);
    } catch (Exception ignored) {}
    return null;
  }
}