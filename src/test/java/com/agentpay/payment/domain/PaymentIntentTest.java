package com.agentpay.payment.domain;

import com.agentpay.agent.domain.AgentId;
import com.agentpay.global.domain.CurrencyCode;
import com.agentpay.global.domain.Money;
import com.agentpay.wallet.domain.WalletId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentIntentTest {

    private static final Instant T0 = Instant.parse("2026-10-01T00:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);

    private static PaymentIntent newIntent() {
        return PaymentIntent.create(AgentId.newId(), WalletId.newId(), new Merchant("m_yes24", "books"),
                Money.of(12_000, CurrencyCode.KRW), "책 구매", new IdempotencyKey("k-1"), T0, TTL);
    }

    @Test
    void startsAsCreated() {
        PaymentIntent intent = newIntent();
        assertThat(intent.status()).isEqualTo(PaymentStatus.CREATED);
        assertThat(intent.expiresAt()).isEqualTo(T0.plus(TTL));
    }

    @Test
    @DisplayName("행복 경로: CREATED → AUTHORIZED → CAPTURED → REFUNDED")
    void happyPath() {
        PaymentIntent intent = newIntent();
        intent.authorize("ref-1", T0);
        assertThat(intent.status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(intent.processorReference()).isEqualTo("ref-1");
        intent.capture(T0.plusSeconds(5));
        assertThat(intent.status()).isEqualTo(PaymentStatus.CAPTURED);
        intent.refund(T0.plusSeconds(10));
        assertThat(intent.status()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("승인 경로: CREATED → PENDING_APPROVAL → AUTHORIZED")
    void approvalPath() {
        PaymentIntent intent = newIntent();
        intent.requireApproval(T0);
        assertThat(intent.status()).isEqualTo(PaymentStatus.PENDING_APPROVAL);
        intent.authorize("ref-1", T0.plusSeconds(60));
        assertThat(intent.status()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    void pendingCanBeRejectedByHuman() {
        PaymentIntent intent = newIntent();
        intent.requireApproval(T0);
        intent.reject("REJECTED_BY_OWNER", T0.plusSeconds(60));
        assertThat(intent.status()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(intent.decisionReason()).isEqualTo("REJECTED_BY_OWNER");
    }

    @Test
    @DisplayName("가승인 없이 매입 불가")
    void cannotCaptureWithoutAuthorization() {
        PaymentIntent intent = newIntent();
        assertThatThrownBy(() -> intent.capture(T0)).isInstanceOf(InvalidStateTransitionException.class);
        intent.requireApproval(T0);
        assertThatThrownBy(() -> intent.capture(T0)).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void cannotRefundUncaptured() {
        PaymentIntent intent = newIntent();
        intent.authorize("ref-1", T0);
        assertThatThrownBy(() -> intent.refund(T0)).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("TTL 경과 시 PENDING_APPROVAL은 EXPIRED, 이후 승인 불가")
    void expiresWhenDue() {
        PaymentIntent intent = newIntent();
        intent.requireApproval(T0);
        assertThat(intent.expireIfDue(T0.plus(TTL).minusSeconds(1))).isFalse();
        assertThat(intent.expireIfDue(T0.plus(TTL))).isTrue();
        assertThat(intent.status()).isEqualTo(PaymentStatus.EXPIRED);
        assertThatThrownBy(() -> intent.authorize("ref", T0.plus(TTL)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void capturedIntentDoesNotExpire() {
        PaymentIntent intent = newIntent();
        intent.authorize("ref-1", T0);
        intent.capture(T0);
        assertThat(intent.expireIfDue(T0.plus(Duration.ofDays(1)))).isFalse();
        assertThat(intent.status()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"REJECTED", "EXPIRED", "REFUNDED"})
    void terminalStatesHaveNoExit(PaymentStatus terminal) {
        assertThat(terminal.isTerminal()).isTrue();
        for (PaymentStatus target : PaymentStatus.values()) {
            assertThat(terminal.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    @DisplayName("멱등성 지문: 같은 내용이면 일치, 금액/가맹점/설명이 다르면 불일치")
    void requestFingerprint() {
        PaymentIntent intent = newIntent();
        Merchant m = new Merchant("m_yes24", "books");
        assertThat(intent.matchesRequest(m, Money.of("12000.00", CurrencyCode.KRW), "책 구매")).isTrue();
        assertThat(intent.matchesRequest(m, Money.of(12_001, CurrencyCode.KRW), "책 구매")).isFalse();
        assertThat(intent.matchesRequest(new Merchant("m_other", "books"), Money.of(12_000, CurrencyCode.KRW), "책 구매"))
                .isFalse();
        assertThat(intent.matchesRequest(m, Money.of(12_000, CurrencyCode.KRW), "다른 설명")).isFalse();
    }
}
