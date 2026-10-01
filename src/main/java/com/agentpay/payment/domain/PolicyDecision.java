package com.agentpay.payment.domain;

/** 지출 정책 평가 결과. */
public record PolicyDecision(Outcome outcome, String reason) {

    public enum Outcome {
        APPROVE,
        REQUIRE_HUMAN_APPROVAL,
        REJECT
    }

    public static PolicyDecision approve() {
        return new PolicyDecision(Outcome.APPROVE, null);
    }

    public static PolicyDecision requireHumanApproval() {
        return new PolicyDecision(Outcome.REQUIRE_HUMAN_APPROVAL, "APPROVAL_THRESHOLD_REACHED");
    }

    public static PolicyDecision reject(String reason) {
        return new PolicyDecision(Outcome.REJECT, reason);
    }
}
