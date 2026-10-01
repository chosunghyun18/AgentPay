package com.agentpay.global.domain;

/**
 * 지원 통화. 소수 자릿수(fractionDigits)는 해당 통화의 최소 단위를 나타낸다.
 * KRW는 원 단위(0자리), USD는 센트 단위(2자리).
 */
public enum CurrencyCode {
    KRW(0),
    USD(2);

    private final int fractionDigits;

    CurrencyCode(int fractionDigits) {
        this.fractionDigits = fractionDigits;
    }

    public int fractionDigits() {
        return fractionDigits;
    }
}
