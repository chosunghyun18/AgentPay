package com.agentpay.payment.domain;

import com.agentpay.global.domain.Money;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 결제 요청 본문의 지문(SHA-256). 같은 Idempotency-Key로 다른 내용을 보내면 충돌로 판정하기 위해 쓴다.
 */
public record RequestFingerprint(String value) {

    public static RequestFingerprint of(Merchant merchant, Money amount, String description) {
        String canonical = String.join("|",
                merchant.id(), merchant.category(),
                amount.amount().toPlainString(), amount.currency().name(),
                description == null ? "" : description);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return new RequestFingerprint(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
