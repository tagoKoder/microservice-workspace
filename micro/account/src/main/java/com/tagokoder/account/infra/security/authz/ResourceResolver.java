package com.tagokoder.account.infra.security.authz;

import com.tagokoder.account.domain.port.out.AccountRepositoryPort;

import java.util.Map;
import java.util.UUID;

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
            String cid = principalCustomerIdOrNull != null ? principalCustomerIdOrNull : "unknown";
            return new ResourceDef("ImaginaryBank::Account", cid, Map.of("owner_customer_id", cid));
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
