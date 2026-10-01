package com.agentpay.payment.infrastructure.processor;

import com.agentpay.payment.domain.PaymentIntent;
import com.agentpay.payment.domain.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Phase 0용 가짜 결제 처리기. 항상 성공하며 실제 돈은 움직이지 않는다.
 * 가맹점 ID가 {@code fail_}로 시작하면 거절을 흉내 낸다 (테스트/데모용).
 *
 * <p>다음 단계: TossPaymentsAdapter / StripeAdapter 로 교체 (설계 문서 5절 참고).
 */
@Component
public class FakePaymentProcessorAdapter implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(FakePaymentProcessorAdapter.class);

    @Override
    public ProcessorResult authorize(PaymentIntent intent) {
        if (intent.merchant().id().startsWith("fail_")) {
            log.info("[fake-processor] authorize declined intent={}", intent.id());
            return ProcessorResult.fail("FAKE_DECLINED");
        }
        String ref = "fake_auth_" + UUID.randomUUID();
        log.info("[fake-processor] authorize intent={} amount={} ref={}", intent.id(), intent.amount(), ref);
        return ProcessorResult.ok(ref);
    }

    @Override
    public ProcessorResult capture(PaymentIntent intent) {
        log.info("[fake-processor] capture intent={} ref={}", intent.id(), intent.processorReference());
        return ProcessorResult.ok(intent.processorReference());
    }

    @Override
    public ProcessorResult refund(PaymentIntent intent) {
        log.info("[fake-processor] refund intent={} ref={}", intent.id(), intent.processorReference());
        return ProcessorResult.ok(intent.processorReference());
    }
}
