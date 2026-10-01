package com.agentpay.payment.domain;

/**
 * 외부 결제 처리기(PG/카드망/스테이블코인 등) 포트.
 * Phase 0은 Fake 어댑터, 이후 Toss Payments / Stripe 등 어댑터로 교체한다.
 */
public interface PaymentProcessor {

    /** 가승인. 성공 시 처리기 측 참조 ID를 반환한다. */
    ProcessorResult authorize(PaymentIntent intent);

    ProcessorResult capture(PaymentIntent intent);

    ProcessorResult refund(PaymentIntent intent);

    record ProcessorResult(boolean success, String reference, String failureCode) {

        public static ProcessorResult ok(String reference) {
            return new ProcessorResult(true, reference, null);
        }

        public static ProcessorResult fail(String failureCode) {
            return new ProcessorResult(false, null, failureCode);
        }
    }
}
