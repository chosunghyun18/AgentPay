package com.agentpay.agent.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * API 키의 SHA-256 해시(hex). 키 자체가 고엔트로피 랜덤값이므로 bcrypt 같은 느린 해시 대신
 * 조회 가능한 결정적 해시를 사용한다. (pepper/HMAC 적용은 키 관리 설계와 함께 Phase 1에서 검토)
 */
public record ApiKeyHash(String value) {

    public ApiKeyHash {
        Objects.requireNonNull(value, "value");
        if (value.length() != 64) {
            throw new IllegalArgumentException("SHA-256 hex 해시는 64자여야 합니다");
        }
    }

    public static ApiKeyHash of(String rawKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return new ApiKeyHash(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
