package com.tagokoder.account.infra.in.grpc.validation;

import bank.accounts.v1.*;
import com.google.protobuf.StringValue;
import com.tagokoder.account.domain.port.in.ReleaseHoldUseCase;
import com.tagokoder.account.domain.port.in.ReserveHoldUseCase;
import com.tagokoder.account.domain.port.in.ValidateAccountsAndLimitsUseCase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tagokoder.account.infra.in.grpc.validation.GrpcValidation.*;

public final class InternalAccountsGrpcValidators {
    private InternalAccountsGrpcValidators() {}

    public static ValidateAccountsAndLimitsUseCase.Command toValidateCommand(ValidateAccountsAndLimitsRequest req) {
        UUID src = requireUuid(req.getSourceAccountId(), "source_account_id");
        UUID dst = requireUuid(req.getDestinationAccountId(), "destination_account_id");
        String currency = requireCurrency(req.getCurrency());
        BigDecimal amount = requirePositiveMoney(req.getAmount(), "amount");
        return new ValidateAccountsAndLimitsUseCase.Command(src, dst, currency, amount);
    }

    public record HoldOpIn(UUID accountId, String currency, BigDecimal amount, String reasonOrNull, UUID holdId, String idempotencyKey) {}

    public static HoldOpIn toReserveHoldInput(ReserveHoldRequest req) {
        UUID accountId = requireUuid(req.getId(), "id");
        if (!req.hasHold()) throw invalid("hold is required");

        String currency = requireCurrency(req.getHold().getCurrency());
        BigDecimal amount = requirePositiveMoney(req.getHold().getAmount(), "amount");

        UUID holdId = requireUuid(req.getHoldId(), "hold_id");
        String idem = requireIdempotencyKey(req.getIdempotencyKey(), "idempotency_key");

        String reason = req.getHold().hasReason() ? optionalTrim(req.getHold().getReason(), "reason", 140) : null;

        return new HoldOpIn(accountId, currency, amount, reason, holdId, idem);
    }

    public static HoldOpIn toReleaseHoldInput(ReleaseHoldRequest req) {
        UUID accountId = requireUuid(req.getId(), "id");
        if (!req.hasHold()) throw invalid("hold is required");

        String currency = requireCurrency(req.getHold().getCurrency());
        BigDecimal amount = requirePositiveMoney(req.getHold().getAmount(), "amount");

        UUID holdId = requireUuid(req.getHoldId(), "hold_id");
        String idem = requireIdempotencyKey(req.getIdempotencyKey(), "idempotency_key");

        String reason = req.getHold().hasReason() ? optionalTrim(req.getHold().getReason(), "reason", 140) : null;

        return new HoldOpIn(accountId, currency, amount, reason, holdId, idem);
    }

    public record BatchIdsIn(List<UUID> ids, List<MissingAccount> missingEarly, boolean includeInactive) {}

    public static BatchIdsIn toBatchIdsInput(BatchGetAccountSummariesRequest req) {
        int n = req.getAccountIdsCount();
        if (n <= 0) throw invalid("account_ids is required");
        if (n > 200) throw invalid("max 200 account_ids");

        boolean includeInactive = req.hasIncludeInactive() && req.getIncludeInactive().getValue();

        List<UUID> ids = new ArrayList<>(n);
        List<MissingAccount> missingEarly = new ArrayList<>();

        for (String raw : req.getAccountIdsList()) {
            // defensivo: evita strings gigantes
            String s = optionalTrim(raw, "account_ids[]", 64);
            if (s == null) {
                missingEarly.add(MissingAccount.newBuilder()
                        .setAccountId("")
                        .setReason(StringValue.of("invalid_uuid"))
                        .build());
                continue;
            }
            try {
                ids.add(UUID.fromString(s));
            } catch (Exception e) {
                missingEarly.add(MissingAccount.newBuilder()
                        .setAccountId(s)
                        .setReason(StringValue.of("invalid_uuid"))
                        .build());
            }
        }

        return new BatchIdsIn(ids, missingEarly, includeInactive);
    }

    // Helpers: convierte HoldOpIn a tus Commands actuales (aunque HOY ignores holdId/idem)
    public static ReserveHoldUseCase.Command toReserveCommand(HoldOpIn in) {
        return new ReserveHoldUseCase.Command(in.accountId(), in.currency(), in.amount(), in.reasonOrNull());
    }

    public static ReleaseHoldUseCase.Command toReleaseCommand(HoldOpIn in) {
        return new ReleaseHoldUseCase.Command(in.accountId(), in.currency(), in.amount(), in.reasonOrNull());
    }
}