package com.agentpay.payment.domain;

import com.agentpay.agent.domain.AgentId;
import com.agentpay.global.domain.Money;
import com.agentpay.wallet.domain.WalletId;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 결제 의도 애그리거트. 에이전트의 결제 요청 1건 = PaymentIntent 1건.
 * 상태 전이는 {@link PaymentStatus#canTransitionTo}로만 허용되며, 위반 시
 * {@link InvalidStateTransitionException}.
 */
public class PaymentIntent {

    private final PaymentIntentId id;
    private final AgentId agentId;
    private final WalletId walletId;
    private final Merchant merchant;
    private final Money amount;
    private final String description;
    private final IdempotencyKey idempotencyKey;
    private final RequestFingerprint fingerprint;
    private PaymentStatus status;
    private String decisionReason;
    private String processorReference;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant updatedAt;

    private PaymentIntent(PaymentIntentId id, AgentId agentId, WalletId walletId, Merchant merchant, Money amount,
                          String description, IdempotencyKey idempotencyKey, RequestFingerprint fingerprint,
                          PaymentStatus status, String decisionReason, String processorReference,
                          Instant createdAt, Instant expiresAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.agentId = Objects.requireNonNull(agentId);
        this.walletId = Objects.requireNonNull(walletId);
        this.merchant = Objects.requireNonNull(merchant);
        this.amount = Objects.requireNonNull(amount);
        this.description = description;
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.fingerprint = Objects.requireNonNull(fingerprint);
        this.status = Objects.requireNonNull(status);
        this.decisionReason = decisionReason;
        this.processorReference = processorReference;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static PaymentIntent create(AgentId agentId, WalletId walletId, Merchant merchant, Money amount,
                                       String description, IdempotencyKey idempotencyKey,
                                       Instant now, Duration ttl) {
        return new PaymentIntent(PaymentIntentId.newId(), agentId, walletId, merchant, amount, description,
                idempotencyKey, RequestFingerprint.of(merchant, amount, description),
                PaymentStatus.CREATED, null, null, now, now.plus(ttl), now);
    }

    public static PaymentIntent reconstitute(PaymentIntentId id, AgentId agentId, WalletId walletId,
                                             Merchant merchant, Money amount, String description,
                                             IdempotencyKey idempotencyKey, RequestFingerprint fingerprint,
                                             PaymentStatus status, String decisionReason,
                                             String processorReference, Instant createdAt,
                                             Instant expiresAt, Instant updatedAt) {
        return new PaymentIntent(id, agentId, walletId, merchant, amount, description, idempotencyKey,
                fingerprint, status, decisionReason, processorReference, createdAt, expiresAt, updatedAt);
    }

    // ---- 멱등성 ----

    /** 같은 Idempotency-Key 재요청이 원 요청과 동일한 내용인지. */
    public boolean matchesRequest(Merchant merchant, Money amount, String description) {
        return fingerprint.equals(RequestFingerprint.of(merchant, amount, description));
    }

    // ---- 상태 전이 ----

    public void authorize(String processorReference, Instant now) {
        transition(PaymentStatus.AUTHORIZED, now);
        this.processorReference = Objects.requireNonNull(processorReference);
    }

    public void requireApproval(Instant now) {
        transition(PaymentStatus.PENDING_APPROVAL, now);
        this.decisionReason = "APPROVAL_THRESHOLD_REACHED";
    }

    public void reject(String reason, Instant now) {
        transition(PaymentStatus.REJECTED, now);
        this.decisionReason = reason;
    }

    public void capture(Instant now) {
        transition(PaymentStatus.CAPTURED, now);
    }

    public void refund(Instant now) {
        transition(PaymentStatus.REFUNDED, now);
    }

    /** TTL이 지났으면 EXPIRED로 전이하고 true를 반환한다. */
    public boolean expireIfDue(Instant now) {
        if (isExpiredAt(now) && status.canTransitionTo(PaymentStatus.EXPIRED)) {
            transition(PaymentStatus.EXPIRED, now);
            return true;
        }
        return false;
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    private void transition(PaymentStatus target, Instant now) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(id, status, target);
        }
        this.status = target;
        this.updatedAt = now;
    }

    public PaymentIntentId id() { return id; }
    public AgentId agentId() { return agentId; }
    public WalletId walletId() { return walletId; }
    public Merchant merchant() { return merchant; }
    public Money amount() { return amount; }
    public String description() { return description; }
    public IdempotencyKey idempotencyKey() { return idempotencyKey; }
    public RequestFingerprint fingerprint() { return fingerprint; }
    public PaymentStatus status() { return status; }
    public String decisionReason() { return decisionReason; }
    public String processorReference() { return processorReference; }
    public Instant createdAt() { return createdAt; }
    public Instant expiresAt() { return expiresAt; }
    public Instant updatedAt() { return updatedAt; }
}
