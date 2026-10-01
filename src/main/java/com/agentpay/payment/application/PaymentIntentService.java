package com.agentpay.payment.application;

import com.agentpay.agent.domain.AgentCredential;
import com.agentpay.agent.domain.AgentId;
import com.agentpay.global.domain.ForbiddenException;
import com.agentpay.global.domain.Money;
import com.agentpay.global.domain.NotFoundException;
import com.agentpay.payment.domain.IdempotencyConflictException;
import com.agentpay.payment.domain.InvalidStateTransitionException;
import com.agentpay.payment.domain.PaymentIntent;
import com.agentpay.payment.domain.PaymentIntentId;
import com.agentpay.payment.domain.PaymentIntentRepository;
import com.agentpay.payment.domain.PaymentProcessor;
import com.agentpay.payment.domain.PaymentProcessor.ProcessorResult;
import com.agentpay.payment.domain.PaymentStatus;
import com.agentpay.payment.domain.PolicyDecision;
import com.agentpay.payment.domain.SpendingPolicyEvaluator;
import com.agentpay.wallet.domain.Wallet;
import com.agentpay.wallet.domain.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 결제 유스케이스.
 *
 * <ul>
 *   <li>create — 에이전트 요청 → 멱등성 확인 → 정책 평가 → (승인 대기 | 가승인 | 거절)</li>
 *   <li>approve / reject — 지갑 소유자(사람)의 결정</li>
 *   <li>capture — 에이전트가 구매 확정 시 매입 + 지갑 차감</li>
 * </ul>
 */
@Service
public class PaymentIntentService {

    private final PaymentIntentRepository intentRepository;
    private final WalletRepository walletRepository;
    private final PaymentProcessor processor;
    private final SpendingPolicyEvaluator evaluator;
    private final PaymentProperties properties;
    private final Clock clock;

    public PaymentIntentService(PaymentIntentRepository intentRepository, WalletRepository walletRepository,
                                PaymentProcessor processor, SpendingPolicyEvaluator evaluator,
                                PaymentProperties properties, Clock clock) {
        this.intentRepository = intentRepository;
        this.walletRepository = walletRepository;
        this.processor = processor;
        this.evaluator = evaluator;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public CreatePaymentIntentResult create(CreatePaymentIntentCommand cmd) {
        AgentCredential agent = cmd.agent();

        Optional<PaymentIntent> existing =
                intentRepository.findByAgentIdAndIdempotencyKey(agent.id(), cmd.idempotencyKey());
        if (existing.isPresent()) {
            PaymentIntent intent = existing.get();
            if (!intent.matchesRequest(cmd.merchant(), cmd.amount(), cmd.description())) {
                throw new IdempotencyConflictException(cmd.idempotencyKey());
            }
            return new CreatePaymentIntentResult(intent, true);
        }

        Instant now = clock.instant();
        PaymentIntent intent = PaymentIntent.create(agent.id(), agent.walletId(), cmd.merchant(), cmd.amount(),
                cmd.description(), cmd.idempotencyKey(), now, properties.intentTtl());

        Money spentToday = intentRepository.sumSpentSince(agent.id(), agent.policy().currency(), startOfToday(now));
        PolicyDecision decision = evaluator.evaluate(agent.policy(), cmd.merchant(), cmd.amount(), spentToday);

        switch (decision.outcome()) {
            case REJECT -> intent.reject(decision.reason(), now);
            case REQUIRE_HUMAN_APPROVAL -> intent.requireApproval(now);
            case APPROVE -> authorizeWithProcessor(intent, now);
        }
        return new CreatePaymentIntentResult(intentRepository.save(intent), false);
    }

    @Transactional
    public PaymentIntent approve(PaymentIntentId id, String ownerId) {
        PaymentIntent intent = loadOwnedByHuman(id, ownerId);
        Instant now = clock.instant();
        if (!intent.expireIfDue(now)) {
            if (intent.status() != PaymentStatus.PENDING_APPROVAL) {
                throw new InvalidStateTransitionException(
                        intent.id(), intent.status(), PaymentStatus.AUTHORIZED);
            }
            authorizeWithProcessor(intent, now);
        }
        return intentRepository.save(intent);
    }

    @Transactional
    public PaymentIntent reject(PaymentIntentId id, String ownerId) {
        PaymentIntent intent = loadOwnedByHuman(id, ownerId);
        intent.reject("REJECTED_BY_OWNER", clock.instant());
        return intentRepository.save(intent);
    }

    @Transactional
    public PaymentIntent capture(PaymentIntentId id, AgentId agentId) {
        PaymentIntent intent = loadOwnedByAgent(id, agentId);
        Instant now = clock.instant();
        if (intent.expireIfDue(now)) {
            return intentRepository.save(intent);
        }
        Wallet wallet = walletRepository.findById(intent.walletId())
                .orElseThrow(() -> new NotFoundException("Wallet", intent.walletId()));
        ProcessorResult result = processor.capture(intent);
        if (!result.success()) {
            throw new PaymentProcessorException(result.failureCode());
        }
        wallet.debit(intent.amount());
        intent.capture(now);
        walletRepository.save(wallet);
        return intentRepository.save(intent);
    }

    @Transactional(readOnly = true)
    public PaymentIntent getForAgent(PaymentIntentId id, AgentId agentId) {
        return loadOwnedByAgent(id, agentId);
    }

    /** 잔액 확인 후 처리기 가승인. 실패 시 REJECTED. */
    private void authorizeWithProcessor(PaymentIntent intent, Instant now) {
        Wallet wallet = walletRepository.findById(intent.walletId())
                .orElseThrow(() -> new NotFoundException("Wallet", intent.walletId()));
        if (!wallet.canAfford(intent.amount())) {
            intent.reject("INSUFFICIENT_BALANCE", now);
            return;
        }
        ProcessorResult result = processor.authorize(intent);
        if (result.success()) {
            intent.authorize(result.reference(), now);
        } else {
            intent.reject("PROCESSOR_DECLINED:" + result.failureCode(), now);
        }
    }

    private PaymentIntent loadOwnedByAgent(PaymentIntentId id, AgentId agentId) {
        PaymentIntent intent = intentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("PaymentIntent", id));
        if (!intent.agentId().equals(agentId)) {
            // 다른 에이전트의 결제 존재 여부를 노출하지 않는다
            throw new NotFoundException("PaymentIntent", id);
        }
        return intent;
    }

    private PaymentIntent loadOwnedByHuman(PaymentIntentId id, String ownerId) {
        PaymentIntent intent = intentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("PaymentIntent", id));
        Wallet wallet = walletRepository.findById(intent.walletId())
                .orElseThrow(() -> new NotFoundException("Wallet", intent.walletId()));
        if (!wallet.isOwnedBy(ownerId)) {
            throw new ForbiddenException("지갑 소유자만 승인/거절할 수 있습니다");
        }
        return intent;
    }

    private Instant startOfToday(Instant now) {
        return LocalDate.ofInstant(now, properties.dailyLimitZone())
                .atStartOfDay(properties.dailyLimitZone())
                .toInstant();
    }
}
