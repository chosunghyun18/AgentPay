package com.agentpay.agent.domain;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 에이전트 API 키 원문. 발급 시 한 번만 노출되고 저장소에는 해시({@link ApiKeyHash})만 남는다.
 * 형식: {@code ap_live_<base64url 32바이트>}
 */
public record ApiKey(String raw) {

    private static final String PREFIX = "ap_live_";
    private static final SecureRandom RANDOM = new SecureRandom();

    public ApiKey {
        if (raw == null || !raw.startsWith(PREFIX) || raw.length() < PREFIX.length() + 32) {
            throw new IllegalArgumentException("유효하지 않은 API 키 형식");
        }
    }

    public static ApiKey generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return new ApiKey(PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    public ApiKeyHash hash() {
        return ApiKeyHash.of(raw);
    }

    @Override
    public String toString() {
        return PREFIX + "****"; // 로그 노출 방지
    }
}
