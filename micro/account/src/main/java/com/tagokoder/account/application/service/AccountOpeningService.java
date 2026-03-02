package com.tagokoder.account.application.service;

import java.math.BigDecimal;
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

    // 0) Leer estado actual
    var ex = openingBonusRepo.findByKey(bonusKey);

    if (ex.isPresent() && ex.get().status() == OpeningBonusRepositoryPort.Status.COMPLETED) {
        var g = ex.get();
        String accNum = accountRepo.findById(g.accountId())
            .map(a -> AccountNumberFmt.fmt12(a.getAccountNumber()))
            .orElse("");
        return new Result(g.accountId(), accNum, g.journalId(), "opened");
    }

    // 1) Si no existe, intentar adquirir (insert PENDING)
    if (ex.isEmpty()) {
        openingBonusRepo.tryAcquire(bonusKey, OPENING_BONUS, currency);
        ex = openingBonusRepo.findByKey(bonusKey);
    }

    var now = ex.orElseThrow(() -> new IllegalStateException("opening bonus grant missing"));

    // 2) Determinar accountId
    UUID accountId = now.accountId();
    String accountNumber;

    if (accountId == null) {
        // este request crea la cuenta y la “ancla” al grant
        var created = createAccount.create(new CreateAccountUseCase.Command(
            customerId, c.productType(), currency
        ));
        accountId = created.accountId();
        accountNumber = AccountNumberFmt.fmt12(created.accountNumber());

        // importante: anclar account_id para reintentos (evita doble cuenta si falla después)
        openingBonusRepo.attachAccountIfEmpty(bonusKey, accountId);
    } else {
        accountNumber = accountRepo.findById(accountId)
            .map(a -> AccountNumberFmt.fmt12(a.getAccountNumber()))
            .orElse("");
    }

    // 3) Postear bono en Ledger (idempotente por bonusKey)
    String journalId = ledger.creditAccountSystem(
        bonusKey,
        accountId,
        currency,
        OPENING_BONUS.toPlainString(),
        c.externalRef(),
        BONUS_REASON
    );

    // 4) Completar PENDING→COMPLETED (solo 1 gana)
    boolean transitioned = openingBonusRepo.completeIfPending(bonusKey, accountId, journalId);

    // 5) Aplicar balance materializado SOLO si esta llamada completó
    if (transitioned) {
        balances.applyCredit(accountId, OPENING_BONUS);
    }

    return new Result(accountId, accountNumber, journalId, "opened");
    }

    private static String normalizeKey(String key, UUID customerId, String productType, String currency) {
        if (key != null && !key.isBlank()) return key.trim();
        String pt = (productType == null) ? "unknown" : productType;
        String cur = (currency == null) ? "XXX" : currency;
        return "open:" + customerId + ":" + pt + ":" + cur;
    }
}