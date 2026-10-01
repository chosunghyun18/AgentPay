package com.agentpay.payment.domain;

/** 결제 대상 가맹점 VO. category는 정책의 카테고리 허용 목록과 매칭된다. */
public record Merchant(String id, String category) {

    public Merchant {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("merchant id는 필수입니다");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("merchant category는 필수입니다");
        }
    }
}
