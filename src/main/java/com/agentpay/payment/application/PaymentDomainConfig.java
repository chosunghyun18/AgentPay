package com.agentpay.payment.application;

import com.agentpay.payment.domain.SpendingPolicyEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 프레임워크 무의존 도메인 서비스를 빈으로 등록한다. */
@Configuration
public class PaymentDomainConfig {

    @Bean
    public SpendingPolicyEvaluator spendingPolicyEvaluator() {
        return new SpendingPolicyEvaluator();
    }
}
