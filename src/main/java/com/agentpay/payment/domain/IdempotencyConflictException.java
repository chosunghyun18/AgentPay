package com.agentpay.payment.domain;

import com.agentpay.global.domain.DomainException;

public class IdempotencyConflictException extends DomainException {

    public IdempotencyConflictException(IdempotencyKey key) {
        super("IDEMPOTENCY_KEY_REUSED", "같은 Idempotency-Key로 다른 요청이 들어왔습니다: " + key.value());
    }
}
