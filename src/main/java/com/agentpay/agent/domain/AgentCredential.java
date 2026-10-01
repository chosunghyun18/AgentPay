package com.agentpay.agent.domain;

import com.agentpay.wallet.domain.WalletId;

import java.time.Instant;
import java.util.Objects;

/**
 * 에이전트 자격 증명 애그리거트. 하나의 지갑에 묶이며, 지출은 {@link SpendingPolicy} 범위로 제한된다.
 * API 키 원문은 저장하지 않는다.
 */
public class AgentCredential {

    private final AgentId id;
    private final WalletId walletId;
    private final String name;
    private final ApiKeyHash apiKeyHash;
    private AgentStatus status;
    private SpendingPolicy policy;
    private final Instant createdAt;

    private AgentCredential(AgentId id, WalletId walletId, String name, ApiKeyHash apiKeyHash,
                            AgentStatus status, SpendingPolicy policy, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.walletId = Objects.requireNonNull(walletId);
        this.name = Objects.requireNonNull(name);
        this.apiKeyHash = Objects.requireNonNull(apiKeyHash);
        this.status = Objects.requireNonNull(status);
        this.policy = Objects.requireNonNull(policy);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static AgentCredential create(WalletId walletId, String name, ApiKeyHash apiKeyHash,
                                         SpendingPolicy policy, Instant now) {
        return new AgentCredential(AgentId.newId(), walletId, name, apiKeyHash, AgentStatus.ACTIVE, policy, now);
    }

    public static AgentCredential reconstitute(AgentId id, WalletId walletId, String name, ApiKeyHash apiKeyHash,
                                               AgentStatus status, SpendingPolicy policy, Instant createdAt) {
        return new AgentCredential(id, walletId, name, apiKeyHash, status, policy, createdAt);
    }

    public boolean isActive() {
        return status == AgentStatus.ACTIVE;
    }

    public void revoke() {
        status = AgentStatus.REVOKED;
    }

    public void changePolicy(SpendingPolicy newPolicy) {
        this.policy = Objects.requireNonNull(newPolicy);
    }

    public AgentId id() { return id; }
    public WalletId walletId() { return walletId; }
    public String name() { return name; }
    public ApiKeyHash apiKeyHash() { return apiKeyHash; }
    public AgentStatus status() { return status; }
    public SpendingPolicy policy() { return policy; }
    public Instant createdAt() { return createdAt; }
}
