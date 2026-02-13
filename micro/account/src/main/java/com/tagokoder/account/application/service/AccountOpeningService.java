package com.tagokoder.account.application.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tagokoder.account.domain.port.in.CreateAccountUseCase;
import com.tagokoder.account.domain.port.in.OpenAccountWithOpeningBonusUseCase;
import com.tagokoder.account.domain.port.out.AccountBalanceRepositoryPort;
import com.tagokoder.account.domain.port.out.LedgerClientPort;
import com.tagokoder.account.domain.port.out.OpeningBonusRepositoryPort;

@Service
public class AccountOpeningService implements OpenAccountWithOpeningBonusUseCase {

    private static final BigDecimal OPENING_BONUS = new BigDecimal("50.00");
    private static final String BONUS_REASON = "registration_bonus";
    private static final String BONUS_EXTERNAL_REF = "bonus:registration";

    private final CreateAccountUseCase createAccount;
    private final LedgerClientPort ledger;
    private final AccountBalanceRepositoryPort balances;
    private final OpeningBonusRepositoryPort openingBonusRepo;

    public AccountOpeningService(
            CreateAccountUseCase createAccount,
            LedgerClientPort ledger,
            AccountBalanceRepositoryPort balances,
            OpeningBonusRepositoryPort openingBonusRepo
    ) {
        this.createAccount = createAccount;
        this.ledger = ledger;
        this.balances = balances;
        this.openingBonusRepo = openingBonusRepo;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Result open(Command c) {
        // 0) validar input mínimo
        UUID customerId = c.customerId();
        String currency = c.currency();
        String initiatedBy = (c.initiatedBy() == null || c.initiatedBy().isBlank()) ? "system" : c.initiatedBy();

        // 1) idempotencyKey base (ideal: REQUIRED)
        String baseKey = normalizeKey(c.idempotencyKey(), customerId, c.productType(), currency);
        String bonusKey = baseKey + ":opening_bonus";

        // 2) Si ya existe grant de bonus, devolvemos determinísticamente
        Optional<OpeningBonusRepositoryPort.BonusGrant> existing = openingBonusRepo.findByKey(bonusKey);
        if (existing.isPresent()) {
            var g = existing.get();
            return new Result(g.accountId(), g.journalId(), "opened");
        }

        // 3) Crear cuenta (TX propia dentro de AccountService.create) -> COMMIT aquí
        var created = createAccount.create(new CreateAccountUseCase.Command(
                customerId,
                c.productType(),
                currency
        ));
        UUID accountId = created.accountId();

        // 4) Postear bono en Ledger (idempotente por bonusKey)
        String journalId = ledger.creditAccount(
                bonusKey,
                accountId,
                currency,
                OPENING_BONUS.toPlainString(), // "50.00"
                initiatedBy,
                BONUS_EXTERNAL_REF,
                BONUS_REASON,
                customerId
        );

        // 5) Guard idempotente + apply snapshot en accounts (TX propia dentro del repo/adapter)
        //    - tryInsert first; si ya existe, NO volver a aplicar saldos.
        var grant = new OpeningBonusRepositoryPort.BonusGrant(
                bonusKey, accountId, journalId, OPENING_BONUS, currency
        );

        boolean inserted = openingBonusRepo.tryInsert(grant);
        if (inserted) {
            // aplicar snapshot +50 ledger/+50 available
            balances.applyCredit(accountId, OPENING_BONUS);
        }

        return new Result(accountId, journalId, "opened");
    }

    private static String normalizeKey(String key, UUID customerId, String productType, String currency) {
        // Si viene vacío, generamos uno determinístico (demo).
        // En prod: mejor exigirlo.
        if (key != null && !key.isBlank()) return key.trim();
        String pt = (productType == null) ? "unknown" : productType;
        String cur = (currency == null) ? "XXX" : currency;
        return "open:" + customerId + ":" + pt + ":" + cur;
    }
}