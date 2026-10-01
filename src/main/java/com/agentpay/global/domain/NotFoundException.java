package com.agentpay.global.domain;

public class NotFoundException extends DomainException {

    public NotFoundException(String resource, Object id) {
        super("NOT_FOUND", resource + "을(를) 찾을 수 없습니다: " + id);
    }
}
