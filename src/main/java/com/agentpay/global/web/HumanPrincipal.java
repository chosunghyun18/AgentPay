package com.agentpay.global.web;

/**
 * 사람(지갑 소유자) 식별 — Phase 0 스텁.
 *
 * <p>현재는 {@code X-Owner-Id} 헤더를 그대로 신뢰한다. <b>운영 사용 불가.</b>
 * Phase 1에서 Spring Security + OAuth2/JWT(또는 passkey) 기반 인증으로 교체한다.
 */
public final class HumanPrincipal {

    public static final String HEADER = "X-Owner-Id";

    private HumanPrincipal() {
    }
}
