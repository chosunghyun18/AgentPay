package com.agentpay.payment.domain;

import java.util.Objects;
import java.util.UUID;

public record PaymentIntentId(UUID value) {

    public PaymentIntentId {
        Objects.requireNonNull(value, "value");
    }

    public static PaymentIntentId newId() {
        return new PaymentIntentId(UUID.randomUUID());
    }

    public static PaymentIntentId of(String value) {
        return new PaymentIntentId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
