package com.agentpay.payment.domain;

import com.agentpay.agent.domain.AgentId;
import com.agentpay.global.domain.CurrencyCode;
import com.agentpay.global.domain.Money;

import java.time.Instant;
import java.util.Optional;

/** PaymentIntent 저장소 포트. */
public interface PaymentIntentRepository {

    PaymentIntent save(PaymentIntent intent);

    Optional<PaymentIntent> findById(PaymentIntentId id);

    Optional<PaymentIntent> findByAgentIdAndIdempotencyKey(AgentId agentId, IdempotencyKey key);

    /** since 이후 생성된 건 중 {@link PaymentStatus#COUNTS_TOWARD_DAILY_LIMIT} 상태 금액 합계. */
    Money sumSpentSince(AgentId agentId, CurrencyCode currency, Instant since);
}
