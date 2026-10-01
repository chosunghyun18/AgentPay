package com.agentpay.global.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 금액 VO (shared kernel). 부동소수점 금지 — BigDecimal만 사용한다.
 *
 * <ul>
 *   <li>음수 금액 불가 (환불/차감은 연산으로 표현)</li>
 *   <li>통화의 최소 단위보다 작은 값은 거부 (예: KRW 100.5) — 암묵적 반올림 금지</li>
 *   <li>서로 다른 통화 간 연산·비교는 예외</li>
 * </ul>
 */
public record Money(BigDecimal amount, CurrencyCode currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("금액은 음수일 수 없습니다: " + amount);
        }
        try {
            amount = amount.setScale(currency.fractionDigits(), RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    currency + "는 소수점 " + currency.fractionDigits() + "자리까지만 허용됩니다: " + amount);
        }
    }

    public static Money of(String amount, CurrencyCode currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money of(long amount, CurrencyCode currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money zero(CurrencyCode currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        BigDecimal result = amount.subtract(other.amount);
        if (result.signum() < 0) {
            throw new IllegalArgumentException("차감 결과가 음수입니다: " + this + " - " + other);
        }
        return new Money(result, currency);
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        return compareTo(other) >= 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (currency != other.currency) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
