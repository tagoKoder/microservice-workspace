package com.tagokoder.account.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tagokoder.account.domain.model.Account;
import com.tagokoder.account.domain.port.in.CreateAccountUseCase;
import com.tagokoder.account.domain.port.in.GetAccountBalancesUseCase;
import com.tagokoder.account.domain.port.in.ListAccountsUseCase;
import com.tagokoder.account.domain.port.in.PatchAccountLimitsUseCase;
import com.tagokoder.account.domain.port.in.ReleaseHoldUseCase;
import com.tagokoder.account.domain.port.in.ReserveHoldUseCase;
import com.tagokoder.account.domain.port.in.ValidateAccountsAndLimitsUseCase;
import com.tagokoder.account.domain.port.out.AccountBalanceRepositoryPort;
import com.tagokoder.account.domain.port.out.AccountLimitsRepositoryPort;
import com.tagokoder.account.domain.port.out.AccountRepositoryPort;

@Service
public class AccountService implements
        CreateAccountUseCase,
        ListAccountsUseCase,
        GetAccountBalancesUseCase,
        PatchAccountLimitsUseCase,
        ValidateAccountsAndLimitsUseCase,
        ReserveHoldUseCase,
        ReleaseHoldUseCase {

    private final AccountRepositoryPort accountRepo;
    private final AccountBalanceRepositoryPort balanceRepo;
    private final AccountLimitsRepositoryPort limitsRepo;

    public AccountService(AccountRepositoryPort accountRepo,
                          AccountBalanceRepositoryPort balanceRepo,
                          AccountLimitsRepositoryPort limitsRepo) {
        this.accountRepo = accountRepo;
        this.balanceRepo = balanceRepo;
        this.limitsRepo = limitsRepo;
    }

    @Override
    @Transactional
    public CreateAccountUseCase.Result create(CreateAccountUseCase.Command c) {
        if (!accountRepo.existsCustomer(c.customerId())) {
            throw new IllegalArgumentException("Customer not found");
        }

        Account a = new Account();
        a.setCustomerId(c.customerId());
        a.setProductType(c.productType());
        a.setCurrency(c.currency());
        a.setStatus("active");
        a.setOpenedAt(Instant.now());
        a.setUpdatedAt(Instant.now());

        Account saved = accountRepo.save(a);

        balanceRepo.initZero(saved.getId());
        limitsRepo.patch(saved.getId(), BigDecimal.ZERO, BigDecimal.ZERO);

        return new CreateAccountUseCase.Result(saved.getId());
    }

    @Override
    public ListAccountsUseCase.Result listByCustomer(UUID customerId) {
        List<Account> accounts = accountRepo.findByCustomerId(customerId);

        var views = accounts.stream().map(a -> {
            var bal = balanceRepo.findByAccountId(a.getId())
                    .orElse(new AccountBalanceRepositoryPort.BalancesRow(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

            return new ListAccountsUseCase.AccountView(
                    a.getId(),
                    a.getCustomerId(),
                    a.getProductType(),
                    a.getCurrency(),
                    a.getStatus(),
                    a.getOpenedAt().atOffset(ZoneOffset.UTC),
                    a.getUpdatedAt().atOffset(ZoneOffset.UTC),
                    new ListAccountsUseCase.Balances(bal.ledger(), bal.available(), bal.hold())
            );
        }).toList();

        return new ListAccountsUseCase.Result(views);
    }

    @Override
    public GetAccountBalancesUseCase.Result get(UUID accountId) {
        var bal = balanceRepo.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account balances not found"));
        return new GetAccountBalancesUseCase.Result(accountId, bal.ledger(), bal.available(), bal.hold());
    }

    @Override
    @Transactional
    public PatchAccountLimitsUseCase.Result patch(PatchAccountLimitsUseCase.Command c) {
        if (c.dailyOut() != null && c.dailyOut().compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("dailyOut must be >= 0");
        if (c.dailyIn() != null && c.dailyIn().compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("dailyIn must be >= 0");

        var row = limitsRepo.patch(c.accountId(), c.dailyOut(), c.dailyIn());
        return new PatchAccountLimitsUseCase.Result(c.accountId(), row.dailyOut(), row.dailyIn());
    }

    
    @Override
    @Transactional(readOnly = true)
    public ValidateAccountsAndLimitsUseCase.Result validate(ValidateAccountsAndLimitsUseCase.Command c) {
        if (c.amount() == null || c.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return new ValidateAccountsAndLimitsUseCase.Result(false, "amount must be > 0");
        }
        if (c.currency() == null || c.currency().length() != 3) {
            return new ValidateAccountsAndLimitsUseCase.Result(false, "invalid currency");
        }

        var srcOpt = accountRepo.findById(c.sourceAccountId());
        if (srcOpt.isEmpty()) return new ValidateAccountsAndLimitsUseCase.Result(false, "source account not found");

        var dstOpt = accountRepo.findById(c.destinationAccountId());
        if (dstOpt.isEmpty()) return new ValidateAccountsAndLimitsUseCase.Result(false, "destination account not found");

        var src = srcOpt.get();
        var dst = dstOpt.get();

        if (!"active".equalsIgnoreCase(src.getStatus())) return new ValidateAccountsAndLimitsUseCase.Result(false, "source not active");
        if (!"active".equalsIgnoreCase(dst.getStatus())) return new ValidateAccountsAndLimitsUseCase.Result(false, "destination not active");

        if (!c.currency().equalsIgnoreCase(src.getCurrency())) return new ValidateAccountsAndLimitsUseCase.Result(false, "source currency mismatch");
        if (!c.currency().equalsIgnoreCase(dst.getCurrency())) return new ValidateAccountsAndLimitsUseCase.Result(false, "destination currency mismatch");

        var bal = balanceRepo.findByAccountId(src.getId())
                .orElse(new AccountBalanceRepositoryPort.BalancesRow(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        if (bal.available().compareTo(c.amount()) < 0) {
            return new ValidateAccountsAndLimitsUseCase.Result(false, "insufficient available");
        }

        // límites (si aplican)
        var limOpt = limitsRepo.findByAccountId(src.getId());
        if (limOpt.isPresent()) {
            var lim = limOpt.get();
            // ejemplo simple: dailyOut debe ser >= amount si dailyOut>0
            if (lim.dailyOut().compareTo(BigDecimal.ZERO) > 0 && c.amount().compareTo(lim.dailyOut()) > 0) {
                return new ValidateAccountsAndLimitsUseCase.Result(false, "dailyOut limit exceeded");
            }
        }

        return new ValidateAccountsAndLimitsUseCase.Result(true, null);
    }

    @Override
    @Transactional
    public ReserveHoldUseCase.Result reserve(ReserveHoldUseCase.Command c) {
        if (c.amount() == null || c.amount().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("amount must be > 0");
        var acc = accountRepo.findById(c.accountId()).orElseThrow(() -> new IllegalArgumentException("account not found"));

        if (!"active".equalsIgnoreCase(acc.getStatus())) throw new IllegalArgumentException("account not active");
        if (!c.currency().equalsIgnoreCase(acc.getCurrency())) throw new IllegalArgumentException("currency mismatch");

        // ATÓMICO: incrementa hold y valida que available sigue suficiente
        var newHold = balanceRepo.incrementHold(c.accountId(), c.amount());

        return new ReserveHoldUseCase.Result(true, newHold);
    }

    @Override
    @Transactional
    public ReleaseHoldUseCase.Result release(ReleaseHoldUseCase.Command c) {
        if (c.amount() == null || c.amount().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("amount must be > 0");
        var acc = accountRepo.findById(c.accountId()).orElseThrow(() -> new IllegalArgumentException("account not found"));

        if (!c.currency().equalsIgnoreCase(acc.getCurrency())) throw new IllegalArgumentException("currency mismatch");

        var newHold = balanceRepo.decrementHold(c.accountId(), c.amount());
        return new ReleaseHoldUseCase.Result(true, newHold);
    }
}
