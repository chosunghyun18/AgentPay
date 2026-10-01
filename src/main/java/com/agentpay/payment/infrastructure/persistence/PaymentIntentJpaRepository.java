package com.agentpay.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PaymentIntentJpaRepository extends JpaRepository<PaymentIntentJpaEntity, UUID> {

    Optional<PaymentIntentJpaEntity> findByAgentIdAndIdempotencyKey(UUID agentId, String idempotencyKey);

    @Query("""
            select coalesce(sum(p.amount), 0) from PaymentIntentJpaEntity p
            where p.agentId = :agentId and p.currency = :currency
              and p.createdAt >= :since and p.status in :statuses
            """)
    BigDecimal sumAmount(@Param("agentId") UUID agentId,
                         @Param("currency") String currency,
                         @Param("since") Instant since,
                         @Param("statuses") Collection<String> statuses);
}
