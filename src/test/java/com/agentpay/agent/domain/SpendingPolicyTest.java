package com.agentpay.agent.domain;

import com.agentpay.global.domain.CurrencyCode;
import com.agentpay.global.domain.Money;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpendingPolicyTest {

    private static Money krw(long v) {
        return Money.of(v, CurrencyCode.KRW);
    }

    @Test
    void emptyAllowlistAllowsAll() {
        SpendingPolicy policy = new SpendingPolicy(krw(100), krw(1000), null, Set.of(), Set.of());
        assertThat(policy.allowsMerchant("any", "any")).isTrue();
    }

    @Test
    void allowlistMatchesMerchantOrCategory() {
        SpendingPolicy policy = new SpendingPolicy(krw(100), krw(1000), null, Set.of("m_aws"), Set.of("books"));
        assertThat(policy.allowsMerchant("m_aws", "cloud")).isTrue();
        assertThat(policy.allowsMerchant("m_yes24", "books")).isTrue();
        assertThat(policy.allowsMerchant("m_casino", "gambling")).isFalse();
    }

    @Test
    void invariants() {
        assertThatThrownBy(() -> new SpendingPolicy(krw(2000), krw(1000), null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SpendingPolicy(krw(100), Money.of(1000, CurrencyCode.USD), null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void approvalThresholdIsInclusive() {
        SpendingPolicy policy = new SpendingPolicy(krw(100), krw(1000), krw(50), null, null);
        assertThat(policy.requiresHumanApproval(krw(49))).isFalse();
        assertThat(policy.requiresHumanApproval(krw(50))).isTrue();
    }
}
