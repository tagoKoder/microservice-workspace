package com.tagokoder.account.infra.in.grpc.validation;

import com.tagokoder.account.domain.port.in.GetAccountByNumberUseCase;
import com.tagokoder.account.domain.port.in.OpenAccountWithOpeningBonusUseCase;
import com.tagokoder.account.domain.port.in.PatchAccountLimitsUseCase;
import com.tagokoder.account.infra.in.grpc.mapper.ProtoEnumMapper;
import com.tagokoder.account.infra.security.context.AuthCtx;
import bank.accounts.v1.*;

import java.util.UUID;

import static com.tagokoder.account.infra.in.grpc.validation.GrpcValidation.*;

public final class AccountsGrpcValidators {
    private AccountsGrpcValidators() {}

    /** ListAccounts: ASVS L3 -> self-only (si hay principal). */
    public static UUID resolveCustomerIdForList(ListAccountsRequest req) {
        var principal = AuthCtx.PRINCIPAL.get();

        // Con auth (prod): usar principal.customer_id
        if (principal != null && principal.customerIdOrNull() != null && !principal.customerIdOrNull().isBlank()) {
            UUID principalId = requireUuid(principal.customerIdOrNull(), "principal.customer_id");

            // si el request manda customer_id, debe coincidir
            if (req.getCustomerId() != null && !req.getCustomerId().isBlank()) {
                UUID requested = requireUuid(req.getCustomerId(), "customer_id");
                if (!requested.equals(principalId)) throw forbidden("Forbidden");
            }
            return principalId;
        }

        // Sin auth (local): permitir customer_id pero validarlo
        return requireUuid(req.getCustomerId(), "customer_id");
    }

    public static GetAccountByNumberUseCase.Command toGetByNumberCommand(GetAccountByNumberRequest req) {
        String acct = requireAccountNumber12(req.getAccountNumber(), "account_number");
        boolean includeInactive = req.hasIncludeInactive() && req.getIncludeInactive().getValue();
        return new GetAccountByNumberUseCase.Command(acct, includeInactive);
    }

    /** CreateAccount: recomendado self-only si hay principal */
    public static OpenAccountWithOpeningBonusUseCase.Command toOpenWithBonusCommand(CreateAccountRequest req) {
        UUID customerId = requireUuid(req.getCustomerId(), "customer_id");
        String productType = ProtoEnumMapper.mapProductType(req.getProductType()); // lanza si UNSPECIFIED
        String currency = requireCurrency(req.getCurrency());

        // idempotency_key: en tu proto está, aquí lo hacemos obligatorio (mutación crítica)
        String idem = requireIdempotencyKey(req.getIdempotencyKey(), "idempotency_key");

        // external_ref opcional (normalizado)
        optionalExternalRef(req.getExternalRef(), "external_ref");

        // Si quieres forzar self-only:
        var principal = AuthCtx.PRINCIPAL.get();
        if (principal != null && principal.customerIdOrNull() != null && !principal.customerIdOrNull().isBlank()) {
            UUID principalId = requireUuid(principal.customerIdOrNull(), "principal.customer_id");
            if (!principalId.equals(customerId)) {
                // si tu modelo soporta admins, aquí podrías exceptuar por rol
                throw forbidden("Forbidden");
            }
        }

        return new OpenAccountWithOpeningBonusUseCase.Command(
                customerId,
                productType,
                currency,
                idem,
                "system"
        );
    }

    public static UUID toAccountId(GetAccountBalancesRequest req) {
        return requireUuid(req.getId(), "id");
    }

    public record PatchLimitsIn(String idempotencyKey, PatchAccountLimitsUseCase.Command command) {}

    public static PatchLimitsIn toPatchLimitsInput(PatchAccountLimitsRequest req, String idempotencyKeyFromCtx) {

        // header requerido (mutación)
        String key = requireIdempotencyKey(idempotencyKeyFromCtx, "idempotency-key");

        UUID accountId = requireUuid(req.getId(), "id");

        boolean hasAny = req.hasDailyOut() || req.hasDailyIn();
        if (!hasAny) throw invalid("At least one of daily_out or daily_in is required");

        var dailyOut = optionalNonNegativeMoney(req.hasDailyOut() ? req.getDailyOut() : null, "daily_out");
        var dailyIn  = optionalNonNegativeMoney(req.hasDailyIn()  ? req.getDailyIn()  : null, "daily_in");

        return new PatchLimitsIn(
                key,
                new PatchAccountLimitsUseCase.Command(accountId, dailyOut, dailyIn)
        );
    }

    public static void enforceSelfOnlyCustomer(UUID ownerCustomerId) {
        var principal = AuthCtx.PRINCIPAL.get();
        if (principal == null) return;

        String cid = principal.customerIdOrNull();
        if (cid == null || cid.isBlank()) return;

        UUID principalId = requireUuid(cid, "principal.customer_id");
        if (!principalId.equals(ownerCustomerId)) {
            throw forbidden("Forbidden");
        }
    }
}