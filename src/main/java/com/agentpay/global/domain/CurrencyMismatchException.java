package com.agentpay.global.domain;

public class CurrencyMismatchException extends DomainException {

    public CurrencyMismatchException(CurrencyCode left, CurrencyCode right) {
        super("CURRENCY_MISMATCH", "통화가 일치하지 않습니다: " + left + " vs " + right);
    }
}
