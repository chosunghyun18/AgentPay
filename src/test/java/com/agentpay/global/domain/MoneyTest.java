package com.agentpay.global.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("스케일이 달라도 같은 금액이면 동등하다 (통화 소수 자릿수로 정규화)")
    void normalizesScale() {
        assertThat(Money.of("1000", CurrencyCode.KRW)).isEqualTo(new Money(new BigDecimal("1000.00"), CurrencyCode.KRW));
        assertThat(Money.of("1.5", CurrencyCode.USD).amount()).isEqualByComparingTo("1.50");
        assertThat(Money.of("1.5", CurrencyCode.USD).amount().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("통화 최소 단위보다 작은 금액은 반올림하지 않고 거부한다")
    void rejectsSubMinorUnit() {
        assertThatThrownBy(() -> Money.of("100.5", CurrencyCode.KRW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.of("0.001", CurrencyCode.USD)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegative() {
        assertThatThrownBy(() -> Money.of("-1", CurrencyCode.KRW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("0.1 + 0.2 == 0.3 (부동소수점 오차 없음)")
    void exactArithmetic() {
        Money sum = Money.of("0.10", CurrencyCode.USD).add(Money.of("0.20", CurrencyCode.USD));
        assertThat(sum).isEqualTo(Money.of("0.30", CurrencyCode.USD));
    }

    @Test
    void subtractAndCompare() {
        Money a = Money.of(10_000, CurrencyCode.KRW);
        Money b = Money.of(3_000, CurrencyCode.KRW);
        assertThat(a.subtract(b)).isEqualTo(Money.of(7_000, CurrencyCode.KRW));
        assertThat(a.isGreaterThan(b)).isTrue();
        assertThat(b.isGreaterThanOrEqual(Money.of(3_000, CurrencyCode.KRW))).isTrue();
        assertThatThrownBy(() -> b.subtract(a)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("다른 통화 간 연산·비교는 예외")
    void currencyMismatch() {
        Money krw = Money.of(1_000, CurrencyCode.KRW);
        Money usd = Money.of(1, CurrencyCode.USD);
        assertThatThrownBy(() -> krw.add(usd)).isInstanceOf(CurrencyMismatchException.class);
        assertThatThrownBy(() -> krw.isGreaterThan(usd)).isInstanceOf(CurrencyMismatchException.class);
    }
}
