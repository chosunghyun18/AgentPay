package com.agentpay.payment.domain;

import com.agentpay.agent.domain.SpendingPolicy;
import com.agentpay.global.domain.Money;

/**
 * 지출 정책 평가 도메인 서비스 (순수 함수).
 *
 * <p>평가 순서 — 먼저 걸리는 규칙이 이긴다:
 * <ol>
 *   <li>통화 불일치 → REJECT</li>
 *   <li>가맹점/카테고리 허용 목록 밖 → REJECT</li>
 *   <li>건당 한도 초과 → REJECT (하드 리밋: 사람 승인으로도 넘을 수 없음)</li>
 *   <li>일일 누적 한도 초과 → REJECT (하드 리밋)</li>
 *   <li>승인 임계 이상 → REQUIRE_HUMAN_APPROVAL</li>
 *   <li>그 외 → APPROVE</li>
 * </ol>
 */
public class SpendingPolicyEvaluator {

    public PolicyDecision evaluate(SpendingPolicy policy, Merchant merchant, Money amount, Money spentToday) {
        if (amount.currency() != policy.currency() || spentToday.currency() != policy.currency()) {
            return PolicyDecision.reject("CURRENCY_MISMATCH");
        }
        if (amount.isZero()) {
            return PolicyDecision.reject("ZERO_AMOUNT");
        }
        if (!policy.allowsMerchant(merchant.id(), merchant.category())) {
            return PolicyDecision.reject("MERCHANT_NOT_ALLOWED");
        }
        if (policy.exceedsPerTransactionLimit(amount)) {
            return PolicyDecision.reject("PER_TRANSACTION_LIMIT_EXCEEDED");
        }
        if (policy.exceedsDailyLimit(spentToday, amount)) {
            return PolicyDecision.reject("DAILY_LIMIT_EXCEEDED");
        }
        if (policy.requiresHumanApproval(amount)) {
            return PolicyDecision.requireHumanApproval();
        }
        return PolicyDecision.approve();
    }
}
