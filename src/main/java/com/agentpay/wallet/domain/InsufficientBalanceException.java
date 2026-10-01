package com.agentpay.wallet.domain;

import com.agentpay.global.domain.DomainException;
import com.agentpay.global.domain.Money;

public class InsufficientBalanceException extends DomainException {

    public InsufficientBalanceException(WalletId walletId, Money balance, Money requested) {
        super("INSUFFICIENT_BALANCE",
                "잔액이 부족합니다 (wallet=" + walletId + ", balance=" + balance + ", requested=" + requested + ")");
    }
}
