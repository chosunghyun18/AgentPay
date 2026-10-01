package com.agentpay.agent.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_credentials")
public class AgentCredentialJpaEntity {

    @Id
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(nullable = false)
    private String name;

    @Column(name = "api_key_hash", nullable = false, length = 64)
    private String apiKeyHash;

    @Column(nullable = false)
    private String status;

    @Column(name = "policy_currency", nullable = false, length = 3)
    private String policyCurrency;

    @Column(name = "per_transaction_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal perTransactionLimit;

    @Column(name = "daily_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyLimit;

    @Column(name = "approval_threshold", precision = 19, scale = 2)
    private BigDecimal approvalThreshold;

    /** 쉼표 구분 목록. 목록이 커지면 별도 테이블로 분리. */
    @Column(name = "allowed_merchants", nullable = false)
    private String allowedMerchants;

    @Column(name = "allowed_categories", nullable = false)
    private String allowedCategories;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AgentCredentialJpaEntity() {
    }

    public AgentCredentialJpaEntity(UUID id, UUID walletId, String name, String apiKeyHash, String status,
                                    String policyCurrency, BigDecimal perTransactionLimit, BigDecimal dailyLimit,
                                    BigDecimal approvalThreshold, String allowedMerchants,
                                    String allowedCategories, Instant createdAt) {
        this.id = id;
        this.walletId = walletId;
        this.name = name;
        this.apiKeyHash = apiKeyHash;
        this.status = status;
        this.policyCurrency = policyCurrency;
        this.perTransactionLimit = perTransactionLimit;
        this.dailyLimit = dailyLimit;
        this.approvalThreshold = approvalThreshold;
        this.allowedMerchants = allowedMerchants;
        this.allowedCategories = allowedCategories;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getWalletId() { return walletId; }
    public String getName() { return name; }
    public String getApiKeyHash() { return apiKeyHash; }
    public String getStatus() { return status; }
    public String getPolicyCurrency() { return policyCurrency; }
    public BigDecimal getPerTransactionLimit() { return perTransactionLimit; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public BigDecimal getApprovalThreshold() { return approvalThreshold; }
    public String getAllowedMerchants() { return allowedMerchants; }
    public String getAllowedCategories() { return allowedCategories; }
    public Instant getCreatedAt() { return createdAt; }
}
