package com.tagokoder.account.infra.security.authz;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.tagokoder.account.domain.port.out.AccountRepositoryPort;
import com.tagokoder.account.infra.security.avp.AvpValues;

import software.amazon.awssdk.services.verifiedpermissions.model.AttributeValue;
import software.amazon.awssdk.services.verifiedpermissions.model.EntityIdentifier;
import software.amazon.awssdk.services.verifiedpermissions.model.EntityItem;

public class AccountResourceTemplates {

  public record Resolved(
      EntityItem resourceEntity,
      List<EntityItem> extraEntities,
      Map<String, AttributeValue> contextAttrs
  ) {}

  private static final String USER_TYPE = "ImaginaryBank::User";
  private static final String ACCOUNT_TYPE = "ImaginaryBank::Account";
  private static final String CUSTOMER_TYPE = "ImaginaryBank::Customer"; // lo vamos a agregar en schema

  private final AccountRepositoryPort accountRepo;

  public AccountResourceTemplates(AccountRepositoryPort accountRepo) {
    this.accountRepo = accountRepo;
  }

  public Resolved resolve(String template, String route, Object request, PrincipalData principal) {
    return switch (template) {
      case RouteAuthzRegistry.T_ACCOUNTS_OF_SELF -> accountsOfSelf(principal);
      case RouteAuthzRegistry.T_ACCOUNT_BY_ID    -> accountById(request);
      case RouteAuthzRegistry.T_ACCOUNT_CREATE   -> accountCreate(request, principal);
      case RouteAuthzRegistry.T_CUSTOMER_CREATE  -> customerCreate(principal);
      case RouteAuthzRegistry.T_CUSTOMER_PATCH   -> customerPatch(request);
      case RouteAuthzRegistry.T_HOLD_ACCOUNT_BY_ID -> accountById(request);
      case RouteAuthzRegistry.T_ACCOUNT_OPEN_BONUS -> accountOpenBonus(request);
      default -> throw new IllegalStateException("Unknown resource template: " + template + " for route=" + route);
    };
  }

  // ---- Template implementations ----

  private Resolved accountsOfSelf(PrincipalData principal) {
    String cid = principal.customerId();
    String rid = "accounts_of:" + cid;

    var attrs = new HashMap<String, AttributeValue>();
    attrs.put("account_id", AvpValues.str(rid));
    attrs.put("owner_customer_id", AvpValues.str(cid));
    attrs.put("status", AvpValues.str("virtual"));

    // también seteo owner como Entity(User) por si mañana quieres policies con owner entity
    attrs.put("owner", AvpValues.entity(USER_TYPE, principal.principalId()));

    EntityIdentifier id = EntityIdentifier.builder().entityType(ACCOUNT_TYPE).entityId(rid).build();
    EntityItem resource = EntityItem.builder().identifier(id).attributes(attrs).build();
    return new Resolved(resource, List.of(), Map.of());
  }

  private Resolved accountById(Object request) {
    UUID accountId = extractUuidFromGetter(request, "getId");
    if (accountId == null) throw new IllegalArgumentException("account id is required");

    var acc = accountRepo.findById(accountId).orElseThrow(() -> new IllegalArgumentException("account not found"));

    String ownerCustomerId = acc.getCustomerId() == null ? "unknown" : acc.getCustomerId().toString();

    var attrs = new HashMap<String, AttributeValue>();
    attrs.put("account_id", AvpValues.str(accountId.toString()));
    attrs.put("owner_customer_id", AvpValues.str(ownerCustomerId));
    attrs.put("status", AvpValues.str(acc.getStatus() == null ? "" : acc.getStatus()));

    EntityIdentifier id = EntityIdentifier.builder().entityType(ACCOUNT_TYPE).entityId(accountId.toString()).build();
    EntityItem resource = EntityItem.builder().identifier(id).attributes(attrs).build();
    return new Resolved(resource, List.of(), Map.of());
  }

  private Resolved accountCreate(Object request, PrincipalData principal) {
    // tu proto CreateAccountRequest normalmente trae customer_id (string uuid)
    String customerId = extractStringFromGetter(request, "getCustomerId");
    if (customerId == null || customerId.isBlank()) customerId = principal.customerId(); // fallback

    // resource “virtual” representando el futuro Account
    String rid = "new_account:" + customerId;

    var attrs = new HashMap<String, AttributeValue>();
    attrs.put("account_id", AvpValues.str(rid));
    attrs.put("owner_customer_id", AvpValues.str(customerId));
    attrs.put("status", AvpValues.str("new"));
    attrs.put("owner", AvpValues.entity(USER_TYPE, principal.principalId()));

    EntityIdentifier id = EntityIdentifier.builder().entityType(ACCOUNT_TYPE).entityId(rid).build();
    EntityItem resource = EntityItem.builder().identifier(id).attributes(attrs).build();
    return new Resolved(resource, List.of(), Map.of());
  }

  private Resolved customerCreate(PrincipalData principal) {
    // customer aún no existe => resource virtual
    String rid = "new_customer:" + principal.principalId();

    var attrs = new HashMap<String, AttributeValue>();
    attrs.put("customer_id", AvpValues.str(rid));
    attrs.put("owner", AvpValues.entity(USER_TYPE, principal.principalId()));
    attrs.put("status", AvpValues.str("new"));

    EntityIdentifier id = EntityIdentifier.builder().entityType(CUSTOMER_TYPE).entityId(rid).build();
    EntityItem resource = EntityItem.builder().identifier(id).attributes(attrs).build();
    return new Resolved(resource, List.of(), Map.of());
  }

  private Resolved customerPatch(Object request) {
    String customerId = extractStringFromGetter(request, "getId"); // o getCustomerId, depende tu proto
    if (customerId == null || customerId.isBlank()) throw new IllegalArgumentException("customer id is required");

    var attrs = new HashMap<String, AttributeValue>();
    attrs.put("customer_id", AvpValues.str(customerId));
    attrs.put("status", AvpValues.str("existing"));

    EntityIdentifier id = EntityIdentifier.builder().entityType(CUSTOMER_TYPE).entityId(customerId).build();
    EntityItem resource = EntityItem.builder().identifier(id).attributes(attrs).build();
    return new Resolved(resource, List.of(), Map.of());
  }

  private Resolved accountOpenBonus(Object request) {
    String customerId = extractStringFromGetter(request, "getCustomerId");
    String externalRef = extractStringFromGetter(request, "getExternalRef");

    if (customerId == null || customerId.isBlank()) throw new IllegalArgumentException("customer_id required");
    if (externalRef == null || externalRef.isBlank()) throw new IllegalArgumentException("external_ref required");

    String rid = "open_bonus:" + customerId + ":" + externalRef;

    var attrs = new HashMap<String, AttributeValue>();
    attrs.put("account_id", AvpValues.str(rid));
    attrs.put("owner_customer_id", AvpValues.str(customerId));
    attrs.put("status", AvpValues.str("onboarding_bonus"));

    var ctx = new HashMap<String, AttributeValue>();
    ctx.put("external_ref", AvpValues.str(externalRef));
    ctx.put("reason", AvpValues.str("registration_bonus"));
    ctx.put("amount", AvpValues.str("50.00"));

    EntityIdentifier id = EntityIdentifier.builder().entityType(ACCOUNT_TYPE).entityId(rid).build();
    EntityItem resource = EntityItem.builder().identifier(id).attributes(attrs).build();
    return new Resolved(resource, List.of(), ctx);
    }

  // ---- reflection helpers (para que pegues rápido) ----
  private static UUID extractUuidFromGetter(Object req, String getter) {
    try {
      var m = req.getClass().getMethod(getter);
      Object v = m.invoke(req);
      if (v instanceof String s && !s.isBlank()) return UUID.fromString(s);
    } catch (Exception ignored) {}
    return null;
  }

  private static String extractStringFromGetter(Object req, String getter) {
    try {
      var m = req.getClass().getMethod(getter);
      Object v = m.invoke(req);
      if (v instanceof String s) return s;
    } catch (Exception ignored) {}
    return null;
  }

  // ---- principal data used by templates ----
  public record PrincipalData(String principalId, String customerId, List<String> roles, boolean mfa) {}
}