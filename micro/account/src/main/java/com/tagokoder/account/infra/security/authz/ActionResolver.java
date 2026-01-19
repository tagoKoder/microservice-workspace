package com.tagokoder.account.infra.security.authz;

import java.util.Map;

public class ActionResolver {

    public record ActionDef(String actionId, boolean critical) {}

    private final Map<String, ActionDef> map = Map.ofEntries(
            Map.entry("bank.accounts.v1.AccountsService/ListAccounts", new ActionDef("accounts:list", false)),
            Map.entry("bank.accounts.v1.AccountsService/CreateAccount", new ActionDef("accounts:create", true)),
            Map.entry("bank.accounts.v1.AccountsService/GetAccountBalances", new ActionDef("accounts:balances_read", false)),
            Map.entry("bank.accounts.v1.AccountsService/PatchAccountLimits", new ActionDef("accounts:limits_patch", true)),

            Map.entry("bank.accounts.v1.CustomersService/CreateCustomer", new ActionDef("customers:create", true)),
            Map.entry("bank.accounts.v1.CustomersService/PatchCustomer", new ActionDef("customers:patch", true)),

            Map.entry("bank.accounts.v1.InternalAccountsService/ValidateAccountsAndLimits", new ActionDef("accounts:validate", false)),
            Map.entry("bank.accounts.v1.InternalAccountsService/ReserveHold", new ActionDef("accounts:hold_reserve", true)),
            Map.entry("bank.accounts.v1.InternalAccountsService/ReleaseHold", new ActionDef("accounts:hold_release", true))
    );

    public ActionDef resolve(String fullMethodName) {
        return map.getOrDefault(fullMethodName, new ActionDef("unknown", true));
    }
}
