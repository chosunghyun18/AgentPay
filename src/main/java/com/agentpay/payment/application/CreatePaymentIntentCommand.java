package com.agentpay.payment.application;

import com.agentpay.agent.domain.AgentCredential;
import com.agentpay.global.domain.Money;
import com.agentpay.payment.domain.IdempotencyKey;
import com.agentpay.payment.domain.Merchant;

public record CreatePaymentIntentCommand(
        AgentCredential agent,
        IdempotencyKey idempotencyKey,
        Merchant merchant,
        Money amount,
        String description
) {
}
