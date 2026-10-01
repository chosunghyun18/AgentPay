package com.agentpay.payment.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * PaymentIntent 상태 머신.
 *
 * <pre>
 *  CREATED ──(정책 통과)──────────────▶ AUTHORIZED ──capture──▶ CAPTURED ──refund──▶ REFUNDED
 *     │                                  ▲    │
 *     ├──(승인 임계 이상)─▶ PENDING_APPROVAL ─┘    │ (TTL 경과)
 *     │                        │  (사람 승인)     ▼
 *     ├──(정책 위반)──────────▶ REJECTED ◀─(사람 거절)
 *     └──(TTL 경과)───────────▶ EXPIRED ◀── PENDING_APPROVAL / AUTHORIZED
 * </pre>
 */
public enum PaymentStatus {
    CREATED,
    PENDING_APPROVAL,
    AUTHORIZED,
    CAPTURED,
    REJECTED,
    EXPIRED,
    REFUNDED;

    /** 일일 한도 집계 대상: 거절·만료·환불된 건은 제외한다. */
    public static final Set<PaymentStatus> COUNTS_TOWARD_DAILY_LIMIT =
            EnumSet.of(PENDING_APPROVAL, AUTHORIZED, CAPTURED);

    public boolean isTerminal() {
        return this == REJECTED || this == EXPIRED || this == REFUNDED;
    }

    public boolean canTransitionTo(PaymentStatus target) {
        return switch (this) {
            case CREATED -> target == AUTHORIZED || target == PENDING_APPROVAL
                    || target == REJECTED || target == EXPIRED;
            case PENDING_APPROVAL -> target == AUTHORIZED || target == REJECTED || target == EXPIRED;
            case AUTHORIZED -> target == CAPTURED || target == EXPIRED;
            case CAPTURED -> target == REFUNDED;
            case REJECTED, EXPIRED, REFUNDED -> false;
        };
    }
}
