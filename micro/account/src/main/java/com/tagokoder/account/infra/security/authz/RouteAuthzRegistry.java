package com.tagokoder.account.infra.security.authz;

import java.util.Map;

public class RouteAuthzRegistry {

  // Templates (strings para no complicarte)
  public static final String T_ACCOUNTS_OF_SELF = "ACCOUNTS_OF_SELF";
  public static final String T_ACCOUNT_BY_ID    = "ACCOUNT_BY_ID";
  public static final String T_ACCOUNT_CREATE   = "ACCOUNT_CREATE";
  public static final String T_CUSTOMER_CREATE  = "CUSTOMER_CREATE";
  public static final String T_CUSTOMER_PATCH   = "CUSTOMER_PATCH";

  public static final String T_HOLD_ACCOUNT_BY_ID = "HOLD_ACCOUNT_BY_ID";
  public static final String T_ACCOUNT_OPEN_BONUS = "ACCOUNT_OPEN_BONUS";

  private final Map<String, RouteDef> routes = Map.ofEntries(
      // ---- Accounts ----
      Map.entry("bank.accounts.v1.AccountsService/ListAccounts",
          new RouteDef("accounts:read", false, AuthzMode.AUTHZ, true, T_ACCOUNTS_OF_SELF)),

      Map.entry("bank.accounts.v1.AccountsService/GetAccountBalances",
          new RouteDef("accounts:balances_read", false, AuthzMode.AUTHZ, true, T_ACCOUNT_BY_ID)),

      Map.entry("bank.accounts.v1.AccountsService/PatchAccountLimits",
          new RouteDef("accounts:limits_patch", true, AuthzMode.AUTHZ, true, T_ACCOUNT_BY_ID)),

      Map.entry("bank.accounts.v1.AccountsService/CreateAccount",
          new RouteDef("accounts:create", true, AuthzMode.PUBLIC, true, T_ACCOUNT_CREATE)),

      // ---- Customers ----
      // Importante: este normalmente ocurre cuando AÚN no hay customer_id (onboarding)
      Map.entry("bank.accounts.v1.CustomersService/CreateCustomer",
          new RouteDef("customers:create", true, AuthzMode.PUBLIC, false, T_CUSTOMER_CREATE)),

      Map.entry("bank.accounts.v1.CustomersService/PatchCustomer",
          new RouteDef("customers:patch", true, AuthzMode.AUTHZ, true, T_CUSTOMER_PATCH)),

      // ---- Internal ----
      Map.entry("bank.accounts.v1.InternalAccountsService/ValidateAccountsAndLimits",
          new RouteDef("accounts:validate", false, AuthzMode.AUTHZ, true, T_ACCOUNT_BY_ID)),

      Map.entry("bank.accounts.v1.InternalAccountsService/ReserveHold",
          new RouteDef("accounts:hold_reserve", true, AuthzMode.AUTHZ, true, T_HOLD_ACCOUNT_BY_ID)),

      Map.entry("bank.accounts.v1.InternalAccountsService/ReleaseHold",
          new RouteDef("accounts:hold_release", true, AuthzMode.AUTHZ, true, T_HOLD_ACCOUNT_BY_ID)),
      Map.entry("bank.accounts.v1.InternalAccountsService/OpenAccountWithOpeningBonus",
        new RouteDef("accounts:open_with_bonus", true, AuthzMode.PUBLIC, false, T_ACCOUNT_OPEN_BONUS))
  );

  public RouteDef get(String fullMethodName) {
    return routes.get(fullMethodName); // null => no registrada => fail-closed
  }
}