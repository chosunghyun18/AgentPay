package com.agentpay.payment.domain;

import com.agentpay.agent.domain.SpendingPolicy;
import com.agentpay.global.domain.CurrencyCode;
import com.agentpay.global.domain.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SpendingPolicyEvaluatorTest {

    private final SpendingPolicyEvaluator evaluator = new SpendingPolicyEvaluator();

    // 건당 10만, 일 30만, 5만 이상 승인 필요, books/cloud 카테고리만 허용
    private final SpendingPolicy policy = new SpendingPolicy(
            krw(100_000), krw(300_000), krw(50_000), Set.of(), Set.of("books", "cloud"));

    private final Merchant bookstore = new Merchant("m_yes24", "books");

    private static Money krw(long v) {
        return Money.of(v, CurrencyCode.KRW);
    }

    @ParameterizedTest(name = "amount={0}, spentToday={1} → {2}")
    @CsvSource({
            "10000,       0, APPROVE",
            "49999,       0, APPROVE",
            "50000,       0, REQUIRE_HUMAN_APPROVAL",
            "100000,      0, REQUIRE_HUMAN_APPROVAL",
            "100001,      0, REJECT",
            "10000,  290000, APPROVE",
            "10001,  290000, REJECT",
    })
    void limitsAndThreshold(long amount, long spentToday, PolicyDecision.Outcome expected) {
        PolicyDecision decision = evaluator.evaluate(policy, bookstore, krw(amount), krw(spentToday));
        assertThat(decision.outcome()).isEqualTo(expected);
    }

    @Test
    void rejectReasons() {
        assertThat(evaluator.evaluate(policy, new Merchant("m_casino", "gambling"), krw(1000), krw(0)).reason())
                .isEqualTo("MERCHANT_NOT_ALLOWED");
        assertThat(evaluator.evaluate(policy, bookstore, krw(100_001), krw(0)).reason())
                .isEqualTo("PER_TRANSACTION_LIMIT_EXCEEDED");
        assertThat(evaluator.evaluate(policy, bookstore, krw(20_000), krw(290_000)).reason())
                .isEqualTo("DAILY_LIMIT_EXCEEDED");
        assertThat(evaluator.evaluate(policy, bookstore, Money.of(10, CurrencyCode.USD), krw(0)).reason())
                .isEqualTo("CURRENCY_MISMATCH");
        assertThat(evaluator.evaluate(policy, bookstore, krw(0), krw(0)).reason())
                .isEqualTo("ZERO_AMOUNT");
    }

    @Test
    void merchantRuleIsCheckedBeforeLimits() {
        PolicyDecision decision = evaluator.evaluate(policy, new Merchant("m_casino", "gambling"),
                krw(1_000_000), krw(0));
        assertThat(decision.reason()).isEqualTo("MERCHANT_NOT_ALLOWED");
    }
}
