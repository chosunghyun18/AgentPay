package com.agentpay.agent.domain;

import com.agentpay.global.domain.CurrencyCode;
import com.agentpay.global.domain.Money;

import java.util.Objects;
import java.util.Set;

/**
 * 사람이 에이전트에게 위임하는 지출 정책 VO.
 *
 * <ul>
 *   <li>{@code perTransactionLimit} — 건당 상한 (초과 시 거절, 하드 리밋)</li>
 *   <li>{@code dailyLimit} — 일일 누적 상한 (초과 시 거절, 하드 리밋)</li>
 *   <li>{@code approvalThreshold} — 이 금액 이상이면 사람 승인 필요 (null이면 승인 불필요)</li>
 *   <li>{@code allowedMerchants} / {@code allowedCategories} — 허용 목록. 둘 다 비어 있으면 전체 허용,
 *       하나라도 지정되면 가맹점 ID 또는 카테고리 중 하나가 일치해야 한다.</li>
 * </ul>
 */
public record SpendingPolicy(
        Money perTransactionLimit,
        Money dailyLimit,
        Money approvalThreshold,
        Set<String> allowedMerchants,
        Set<String> allowedCategories
) {

    public SpendingPolicy {
        Objects.requireNonNull(perTransactionLimit, "perTransactionLimit");
        Objects.requireNonNull(dailyLimit, "dailyLimit");
        CurrencyCode currency = perTransactionLimit.currency();
        if (dailyLimit.currency() != currency
                || (approvalThreshold != null && approvalThreshold.currency() != currency)) {
            throw new IllegalArgumentException("정책의 모든 한도는 같은 통화여야 합니다");
        }
        if (perTransactionLimit.isGreaterThan(dailyLimit)) {
            throw new IllegalArgumentException("건당 한도는 일일 한도를 넘을 수 없습니다");
        }
        allowedMerchants = allowedMerchants == null ? Set.of() : Set.copyOf(allowedMerchants);
        allowedCategories = allowedCategories == null ? Set.of() : Set.copyOf(allowedCategories);
    }

    public CurrencyCode currency() {
        return perTransactionLimit.currency();
    }

    public boolean allowsMerchant(String merchantId, String category) {
        if (allowedMerchants.isEmpty() && allowedCategories.isEmpty()) {
            return true;
        }
        return allowedMerchants.contains(merchantId) || allowedCategories.contains(category);
    }

    public boolean exceedsPerTransactionLimit(Money amount) {
        return amount.isGreaterThan(perTransactionLimit);
    }

    public boolean exceedsDailyLimit(Money spentToday, Money amount) {
        return spentToday.add(amount).isGreaterThan(dailyLimit);
    }

    public boolean requiresHumanApproval(Money amount) {
        return approvalThreshold != null && amount.isGreaterThanOrEqual(approvalThreshold);
    }
}
