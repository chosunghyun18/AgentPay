package com.agentpay.global.domain;

/** 도메인 규칙 위반의 공통 상위 예외. code는 API 오류 응답에 그대로 노출된다. */
public class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
