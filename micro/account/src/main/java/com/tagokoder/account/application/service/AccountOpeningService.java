package com.tagokoder.account.application.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tagokoder.account.application.AccountNumberFmt;
import com.tagokoder.account.domain.port.in.CreateAccountUseCase;
import com.tagokoder.account.domain.port.in.OpenAccountWithOpeningBonusUseCase;
import com.tagokoder.account.domain.port.out.AccountBalanceRepositoryPort;
import com.tagokoder.account.domain.port.out.AccountRepositoryPort;
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
    private final AccountRepositoryPort accountRepo;

    public AccountOpeningService(
            CreateAccountUseCase createAccount,
            LedgerClientPort ledger,
            AccountBalanceRepositoryPort balances,
            OpeningBonusRepositoryPort openingBonusRepo,
            AccountRepositoryPort accountRepo
    ) {
        this.createAccount = createAccount;
        this.ledger = ledger;
        this.balances = balances;
        this.openingBonusRepo = openingBonusRepo;
        this.accountRepo = accountRepo;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Result open(Command c) {

        UUID customerId = c.customerId();
        String currency = c.currency();
        String initiatedBy = (c.initiatedBy() == null || c.initiatedBy().isBlank()) ? "system" : c.initiatedBy();

        String baseKey = normalizeKey(c.idempotencyKey(), customerId, c.productType(), currency);
        String bonusKey = baseKey + ":opening_bonus";

        // 1) Si ya existe el grant, devolvemos determinístico (incluye account_number)
        Optional<OpeningBonusRepositoryPort.BonusGrant> existing = openingBonusRepo.findByKey(bonusKey);
        if (existing.isPresent()) {
            var g = existing.get();
            String accNum = accountRepo.findById(g.accountId())
                    .map(a -> AccountNumberFmt.fmt12(a.getAccountNumber()))
                    .orElse("");
            return new Result(g.accountId(), accNum, g.journalId(), "opened");
        }

        // 2) Crear cuenta
        var created = createAccount.create(new CreateAccountUseCase.Command(
                customerId,
                c.productType(),
                currency
        ));

        UUID accountId = created.accountId();
        String accountNumber = AccountNumberFmt.fmt12(created.accountNumber());

        // 3) Postear bono (idempotente por bonusKey)
        String journalId = ledger.creditAccount(
                bonusKey,
                accountId,
                currency,
                OPENING_BONUS.toPlainString(),
                initiatedBy,
                BONUS_EXTERNAL_REF,
                BONUS_REASON,
                customerId
        );

        // 4) Guardar grant (idempotente)
        var grant = new OpeningBonusRepositoryPort.BonusGrant(
                bonusKey, accountId, journalId, OPENING_BONUS, currency
        );

        boolean inserted = openingBonusRepo.tryInsert(grant);

        if (inserted) {
            balances.applyCredit(accountId, OPENING_BONUS);
            return new Result(accountId, accountNumber, journalId, "opened");
        }

        // Carrera: otro request ganó. Re-lee y devuelve el “real”
        var existingAfter = openingBonusRepo.findByKey(bonusKey);
        if (existingAfter.isPresent()) {
            var g = existingAfter.get();
            String accNum = accountRepo.findById(g.accountId())
                    .map(a -> AccountNumberFmt.fmt12(a.getAccountNumber()))
                    .orElse("");
            return new Result(g.accountId(), accNum, g.journalId(), "opened");
        }

        throw new IllegalStateException("opening bonus grant missing after concurrent insert");
    }

    private static String normalizeKey(String key, UUID customerId, String productType, String currency) {
        if (key != null && !key.isBlank()) return key.trim();
        String pt = (productType == null) ? "unknown" : productType;
        String cur = (currency == null) ? "XXX" : currency;
        return "open:" + customerId + ":" + pt + ":" + cur;
    }
}