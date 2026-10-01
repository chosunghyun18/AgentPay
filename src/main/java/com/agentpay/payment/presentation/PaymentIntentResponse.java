package com.agentpay.payment.presentation;

import com.agentpay.payment.domain.PaymentIntent;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentIntentResponse(
        String id,
        String agentId,
        String walletId,
        String merchantId,
        String merchantCategory,
        BigDecimal amount,
        String currency,
        String description,
        String status,
        String decisionReason,
        Instant createdAt,
        Instant expiresAt
) {

    public static PaymentIntentResponse from(PaymentIntent intent) {
        return new PaymentIntentResponse(
                intent.id().toString(),
                intent.agentId().toString(),
                intent.walletId().toString(),
                intent.merchant().id(),
                intent.merchant().category(),
                intent.amount().amount(),
                intent.amount().currency().name(),
                intent.description(),
                intent.status().name(),
                intent.decisionReason(),
                intent.createdAt(),
                intent.expiresAt());
    }
}
