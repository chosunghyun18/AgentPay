package com.agentpay.payment.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_intents")
public class PaymentIntentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "merchant_category", nullable = false)
    private String merchantCategory;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    private String description;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(nullable = false)
    private String status;

    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "processor_reference")
    private String processorReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentIntentJpaEntity() {
    }

    public PaymentIntentJpaEntity(UUID id, UUID agentId, UUID walletId, String merchantId, String merchantCategory,
                                  BigDecimal amount, String currency, String description, String idempotencyKey,
                                  String requestFingerprint, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.agentId = agentId;
        this.walletId = walletId;
        this.merchantId = merchantId;
        this.merchantCategory = merchantCategory;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /** 가변 필드(상태 관련)만 갱신한다. */
    public void updateState(String status, String decisionReason, String processorReference, Instant updatedAt) {
        this.status = status;
        this.decisionReason = decisionReason;
        this.processorReference = processorReference;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getAgentId() { return agentId; }
    public UUID getWalletId() { return walletId; }
    public String getMerchantId() { return merchantId; }
    public String getMerchantCategory() { return merchantCategory; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public String getStatus() { return status; }
    public String getDecisionReason() { return decisionReason; }
    public String getProcessorReference() { return processorReference; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
