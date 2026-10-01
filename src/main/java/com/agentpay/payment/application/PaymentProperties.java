package com.agentpay.payment.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.ZoneId;

@ConfigurationProperties(prefix = "agentpay.payment")
public record PaymentProperties(Duration intentTtl, ZoneId dailyLimitZone) {

    public PaymentProperties {
        if (intentTtl == null) {
            intentTtl = Duration.ofMinutes(30);
        }
        if (dailyLimitZone == null) {
            dailyLimitZone = ZoneId.of("Asia/Seoul");
        }
    }
}
