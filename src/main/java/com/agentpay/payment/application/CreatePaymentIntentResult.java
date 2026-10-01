package com.agentpay.payment.application;

import com.agentpay.payment.domain.PaymentIntent;

/** replayed=true면 같은 Idempotency-Key의 기존 결과를 그대로 돌려준 것이다. */
public record CreatePaymentIntentResult(PaymentIntent intent, boolean replayed) {
}
