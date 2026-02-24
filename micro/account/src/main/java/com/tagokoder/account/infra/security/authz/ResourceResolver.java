package com.tagokoder.account.infra.security.authz;

import java.util.Map;
import java.util.UUID;

import com.tagokoder.account.domain.port.out.AccountRepositoryPort;

public class ResourceResolver {

    public record ResourceDef(String type, String id, Map<String, Object> attrs) {}

    private final AccountRepositoryPort accountRepo;

    public ResourceResolver(AccountRepositoryPort accountRepo) {
        this.accountRepo = accountRepo;
    }

    public ResourceDef resolve(String fullMethodName, Object request, String principalCustomerIdOrNull) {
        // Default: resource = "System"
        if (fullMethodName.equals("bank.accounts.v1.AccountsService/ListAccounts")) {
            // Recurso: Customer (self)
            String cid = (principalCustomerIdOrNull != null && !principalCustomerIdOrNull.isBlank())
            ? principalCustomerIdOrNull
            : "unknown";

            String rid = "accounts_of:" + cid;
            return new ResourceDef("ImaginaryBank::Account", rid, Map.of(
            "owner_customer_id", cid,
            "account_id", rid
            ));
        }

        // Para requests con accountId: usamos owner = account.customerId
        UUID accountId = extractAccountId(request);
        if (accountId != null) {
            var acc = accountRepo.findById(accountId).orElse(null);
            String owner = (acc != null && acc.getCustomerId() != null) ? acc.getCustomerId().toString() : "unknown";

            return new ResourceDef("ImaginaryBank::Account", accountId.toString(), Map.of(
                    "owner_customer_id", owner
            ));
        }

        return new ResourceDef("System", "system", Map.of());
    }

    private UUID extractAccountId(Object request) {
        try {
            // bank.accounts.v1.GetAccountBalancesRequest { string id = ... }
            var m = request.getClass().getMethod("getId");
            Object v = m.invoke(request);
            if (v instanceof String s && !s.isBlank()) return UUID.fromString(s);
        } catch (Exception ignored) {}

        // CreateAccountRequest no: usa customerId (se maneja por self-policy)
        return null;
    }
}
