package com.agentpay.wallet.domain;

import com.agentpay.global.domain.Money;

import java.time.Instant;
import java.util.Objects;

/**
 * 지갑 애그리거트. 소유자는 사람(human principal)이며, 에이전트는 지갑을 소유하지 않고
 * 위임된 정책(SpendingPolicy) 범위 내에서만 사용한다.
 *
 * <p>Phase 0은 단일 통화 지갑 + 즉시 차감 방식. 승인 시점 홀드(가승인 금액 묶기)는 Phase 1 과제.
 */
public class Wallet {

    private final WalletId id;
    private final String ownerId;
    private Money balance;
    private final Instant createdAt;

    private Wallet(WalletId id, String ownerId, Money balance, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.ownerId = requireText(ownerId);
        this.balance = Objects.requireNonNull(balance);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static Wallet create(String ownerId, Money initialBalance, Instant now) {
        return new Wallet(WalletId.newId(), ownerId, initialBalance, now);
    }

    public static Wallet reconstitute(WalletId id, String ownerId, Money balance, Instant createdAt) {
        return new Wallet(id, ownerId, balance, createdAt);
    }

    public boolean isOwnedBy(String principalId) {
        return ownerId.equals(principalId);
    }

    public boolean canAfford(Money amount) {
        return balance.isGreaterThanOrEqual(amount);
    }

    public void deposit(Money amount) {
        balance = balance.add(amount);
    }

    public void debit(Money amount) {
        if (!canAfford(amount)) {
            throw new InsufficientBalanceException(id, balance, amount);
        }
        balance = balance.subtract(amount);
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ownerId는 필수입니다");
        }
        return value;
    }

    public WalletId id() { return id; }
    public String ownerId() { return ownerId; }
    public Money balance() { return balance; }
    public Instant createdAt() { return createdAt; }
}
