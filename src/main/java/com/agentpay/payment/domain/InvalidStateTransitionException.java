package com.agentpay.payment.domain;

import com.agentpay.global.domain.DomainException;

public class InvalidStateTransitionException extends DomainException {

    public InvalidStateTransitionException(PaymentIntentId id, PaymentStatus from, PaymentStatus to) {
        super("INVALID_STATE_TRANSITION", "PaymentIntent " + id + ": " + from + " → " + to + " 전이는 허용되지 않습니다");
    }
}
