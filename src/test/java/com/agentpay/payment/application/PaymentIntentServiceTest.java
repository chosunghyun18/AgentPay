package com.agentpay.payment.application;

import com.agentpay.agent.domain.AgentCredential;
import com.agentpay.agent.domain.AgentId;
import com.agentpay.agent.domain.ApiKey;
import com.agentpay.agent.domain.SpendingPolicy;
import com.agentpay.global.domain.CurrencyCode;
import com.agentpay.global.domain.ForbiddenException;
import com.agentpay.global.domain.Money;
import com.agentpay.payment.domain.IdempotencyConflictException;
import com.agentpay.payment.domain.IdempotencyKey;
import com.agentpay.payment.domain.Merchant;
import com.agentpay.payment.domain.PaymentIntent;
import com.agentpay.payment.domain.PaymentIntentId;
import com.agentpay.payment.domain.PaymentIntentRepository;
import com.agentpay.payment.domain.PaymentProcessor;
import com.agentpay.payment.domain.PaymentStatus;
import com.agentpay.payment.domain.SpendingPolicyEvaluator;
import com.agentpay.wallet.domain.Wallet;
import com.agentpay.wallet.domain.WalletId;
import com.agentpay.wallet.domain.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 프레임워크 없이 in-memory 포트 구현으로 유스케이스를 검증한다. */
class PaymentIntentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T03:00:00Z");

    private InMemoryIntents intents;
    private InMemoryWallets wallets;
    private CountingProcessor processor;
    private PaymentIntentService service;
    private Wallet wallet;
    private AgentCredential agent;

    @BeforeEach
    void setUp() {
        intents = new InMemoryIntents();
        wallets = new InMemoryWallets();
        processor = new CountingProcessor();
        service = new PaymentIntentService(intents, wallets, processor, new SpendingPolicyEvaluator(),
                new PaymentProperties(Duration.ofMinutes(30), ZoneId.of("Asia/Seoul")),
                Clock.fixed(NOW, ZoneOffset.UTC));

        wallet = wallets.save(Wallet.create("owner-1", krw(200_000), NOW));
        SpendingPolicy policy = new SpendingPolicy(krw(100_000), krw(150_000), krw(50_000), Set.of(), Set.of());
        agent = AgentCredential.create(wallet.id(), "agent", ApiKey.generate().hash(), policy, NOW);
    }

    private static Money krw(long v) {
        return Money.of(v, CurrencyCode.KRW);
    }

    private CreatePaymentIntentCommand cmd(String key, long amount) {
        return new CreatePaymentIntentCommand(agent, new IdempotencyKey(key),
                new Merchant("m_shop", "books"), krw(amount), "구매");
    }

    @Test
    void smallPaymentIsAuthorizedImmediately() {
        CreatePaymentIntentResult result = service.create(cmd("k1", 10_000));
        assertThat(result.replayed()).isFalse();
        assertThat(result.intent().status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(processor.authorizeCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 키 + 같은 요청 재시도 → 기존 결과 재생, 처리기 재호출 없음")
    void idempotentReplay() {
        PaymentIntent first = service.create(cmd("k1", 10_000)).intent();
        CreatePaymentIntentResult second = service.create(cmd("k1", 10_000));
        assertThat(second.replayed()).isTrue();
        assertThat(second.intent().id()).isEqualTo(first.id());
        assertThat(processor.authorizeCalls.get()).isEqualTo(1);
        assertThat(intents.store).hasSize(1);
    }

    @Test
    @DisplayName("같은 키 + 다른 요청 → 충돌")
    void idempotencyConflict() {
        service.create(cmd("k1", 10_000));
        assertThatThrownBy(() -> service.create(cmd("k1", 20_000)))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void largePaymentWaitsForHumanThenOwnerApproves() {
        PaymentIntent pending = service.create(cmd("k1", 60_000)).intent();
        assertThat(pending.status()).isEqualTo(PaymentStatus.PENDING_APPROVAL);
        assertThat(processor.authorizeCalls.get()).isZero();

        assertThatThrownBy(() -> service.approve(pending.id(), "someone-else"))
                .isInstanceOf(ForbiddenException.class);

        PaymentIntent approved = service.approve(pending.id(), "owner-1");
        assertThat(approved.status()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    @DisplayName("일일 한도: 대기·가승인 건도 누적에 포함된다")
    void dailyLimitCountsPendingAndAuthorized() {
        service.create(cmd("k1", 60_000));  // PENDING_APPROVAL
        service.create(cmd("k2", 40_000));  // AUTHORIZED
        PaymentIntent third = service.create(cmd("k3", 49_000)).intent(); // 누적 149,000 → 허용
        assertThat(third.status()).isEqualTo(PaymentStatus.AUTHORIZED);
        PaymentIntent fourth = service.create(cmd("k4", 2_000)).intent(); // 151,000 → 거절
        assertThat(fourth.status()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(fourth.decisionReason()).isEqualTo("DAILY_LIMIT_EXCEEDED");
    }

    @Test
    void captureDebitsWallet() {
        PaymentIntent intent = service.create(cmd("k1", 10_000)).intent();
        PaymentIntent captured = service.capture(intent.id(), agent.id());
        assertThat(captured.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(wallets.findById(wallet.id()).orElseThrow().balance()).isEqualTo(krw(190_000));
    }

    @Test
    void insufficientBalanceRejects() {
        Wallet poor = wallets.save(Wallet.create("owner-2", krw(5_000), NOW));
        agent = AgentCredential.create(poor.id(), "agent2", ApiKey.generate().hash(), agent.policy(), NOW);
        PaymentIntent intent = service.create(cmd("k1", 10_000)).intent();
        assertThat(intent.status()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(intent.decisionReason()).isEqualTo("INSUFFICIENT_BALANCE");
    }

    // ---- in-memory 포트 구현 ----

    static class InMemoryIntents implements PaymentIntentRepository {
        final Map<PaymentIntentId, PaymentIntent> store = new HashMap<>();

        @Override
        public PaymentIntent save(PaymentIntent intent) {
            store.put(intent.id(), intent);
            return intent;
        }

        @Override
        public Optional<PaymentIntent> findById(PaymentIntentId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<PaymentIntent> findByAgentIdAndIdempotencyKey(AgentId agentId, IdempotencyKey key) {
            return store.values().stream()
                    .filter(i -> i.agentId().equals(agentId) && i.idempotencyKey().equals(key))
                    .findFirst();
        }

        @Override
        public Money sumSpentSince(AgentId agentId, CurrencyCode currency, Instant since) {
            return store.values().stream()
                    .filter(i -> i.agentId().equals(agentId) && !i.createdAt().isBefore(since))
                    .filter(i -> PaymentStatus.COUNTS_TOWARD_DAILY_LIMIT.contains(i.status()))
                    .map(PaymentIntent::amount)
                    .reduce(Money.zero(currency), Money::add);
        }
    }

    static class InMemoryWallets implements WalletRepository {
        final Map<WalletId, Wallet> store = new HashMap<>();

        @Override
        public Wallet save(Wallet wallet) {
            store.put(wallet.id(), wallet);
            return wallet;
        }

        @Override
        public Optional<Wallet> findById(WalletId id) {
            return Optional.ofNullable(store.get(id));
        }
    }

    static class CountingProcessor implements PaymentProcessor {
        final AtomicInteger authorizeCalls = new AtomicInteger();

        @Override
        public ProcessorResult authorize(PaymentIntent intent) {
            return ProcessorResult.ok("ref-" + authorizeCalls.incrementAndGet());
        }

        @Override
        public ProcessorResult capture(PaymentIntent intent) {
            return ProcessorResult.ok(intent.processorReference());
        }

        @Override
        public ProcessorResult refund(PaymentIntent intent) {
            return ProcessorResult.ok(intent.processorReference());
        }
    }
}
