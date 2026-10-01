package com.agentpay.payment.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentIntentRequest(
        @NotBlank @Size(max = 100) String merchantId,
        @NotBlank @Size(max = 100) String merchantCategory,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @Size(max = 500) String description
) {
}
