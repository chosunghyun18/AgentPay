package com.agentpay.payment.infrastructure.persistence;

import com.agentpay.agent.domain.AgentId;
import com.agentpay.global.domain.CurrencyCode;
import com.agentpay.global.domain.Money;
import com.agentpay.payment.domain.IdempotencyKey;
import com.agentpay.payment.domain.Merchant;
import com.agentpay.payment.domain.PaymentIntent;
import com.agentpay.payment.domain.PaymentIntentId;
import com.agentpay.payment.domain.PaymentIntentRepository;
import com.agentpay.payment.domain.PaymentStatus;
import com.agentpay.payment.domain.RequestFingerprint;
import com.agentpay.wallet.domain.WalletId;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class PaymentIntentRepositoryAdapter implements PaymentIntentRepository {

    private static final List<String> SPENT_STATUSES =
            PaymentStatus.COUNTS_TOWARD_DAILY_LIMIT.stream().map(Enum::name).toList();

    private final PaymentIntentJpaRepository jpaRepository;

    public PaymentIntentRepositoryAdapter(PaymentIntentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PaymentIntent save(PaymentIntent intent) {
        PaymentIntentJpaEntity entity = jpaRepository.findById(intent.id().value())
                .orElseGet(() -> new PaymentIntentJpaEntity(
                        intent.id().value(),
                        intent.agentId().value(),
                        intent.walletId().value(),
                        intent.merchant().id(),
                        intent.merchant().category(),
                        intent.amount().amount(),
                        intent.amount().currency().name(),
                        intent.description(),
                        intent.idempotencyKey().value(),
                        intent.fingerprint().value(),
                        intent.createdAt(),
                        intent.expiresAt()));
        entity.updateState(intent.status().name(), intent.decisionReason(),
                intent.processorReference(), intent.updatedAt());
        jpaRepository.saveAndFlush(entity); // 유니크 제약(멱등 키) 위반을 트랜잭션 안에서 즉시 감지
        return intent;
    }

    @Override
    public Optional<PaymentIntent> findById(PaymentIntentId id) {
        return jpaRepository.findById(id.value()).map(PaymentIntentRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<PaymentIntent> findByAgentIdAndIdempotencyKey(AgentId agentId, IdempotencyKey key) {
        return jpaRepository.findByAgentIdAndIdempotencyKey(agentId.value(), key.value())
                .map(PaymentIntentRepositoryAdapter::toDomain);
    }

    @Override
    public Money sumSpentSince(AgentId agentId, CurrencyCode currency, Instant since) {
        BigDecimal sum = jpaRepository.sumAmount(agentId.value(), currency.name(), since, SPENT_STATUSES);
        return new Money(sum == null ? BigDecimal.ZERO : sum, currency);
    }

    private static PaymentIntent toDomain(PaymentIntentJpaEntity e) {
        return PaymentIntent.reconstitute(
                new PaymentIntentId(e.getId()),
                new AgentId(e.getAgentId()),
                new WalletId(e.getWalletId()),
                new Merchant(e.getMerchantId(), e.getMerchantCategory()),
                new Money(e.getAmount(), CurrencyCode.valueOf(e.getCurrency())),
                e.getDescription(),
                new IdempotencyKey(e.getIdempotencyKey()),
                new RequestFingerprint(e.getRequestFingerprint()),
                PaymentStatus.valueOf(e.getStatus()),
                e.getDecisionReason(),
                e.getProcessorReference(),
                e.getCreatedAt(),
                e.getExpiresAt(),
                e.getUpdatedAt());
    }
}
