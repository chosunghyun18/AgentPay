package com.agentpay.payment.domain;

/**
 * 에이전트가 보내는 Idempotency-Key. 에이전트 단위로 유일하다 (agent_id, key).
 * LLM 에이전트는 타임아웃 후 같은 호출을 재시도하는 일이 잦으므로 결제 생성에 필수로 요구한다.
 */
public record IdempotencyKey(String value) {

    public static final int MAX_LENGTH = 255;

    public IdempotencyKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key는 필수입니다");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Idempotency-Key는 " + MAX_LENGTH + "자 이하여야 합니다");
        }
    }
}
