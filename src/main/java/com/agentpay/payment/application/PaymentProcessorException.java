package com.agentpay.payment.application;

import com.agentpay.global.domain.DomainException;

public class PaymentProcessorException extends DomainException {

    public PaymentProcessorException(String failureCode) {
        super("PROCESSOR_FAILURE", "결제 처리기 오류: " + failureCode);
    }
}
