-- AgentPay 초기 스키마
-- 애그리거트 간 참조는 FK 제약 없이 UUID 컬럼으로만 둔다 (도메인 규칙: ID 참조).

CREATE TABLE wallets (
    id              UUID            PRIMARY KEY,
    owner_id        VARCHAR(100)    NOT NULL,
    currency        VARCHAR(3)      NOT NULL,
    balance         NUMERIC(19, 2)  NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_wallets_owner ON wallets (owner_id);

CREATE TABLE agent_credentials (
    id                      UUID            PRIMARY KEY,
    wallet_id               UUID            NOT NULL,
    name                    VARCHAR(100)    NOT NULL,
    api_key_hash            VARCHAR(64)     NOT NULL,
    status                  VARCHAR(20)     NOT NULL,
    policy_currency         VARCHAR(3)      NOT NULL,
    per_transaction_limit   NUMERIC(19, 2)  NOT NULL,
    daily_limit             NUMERIC(19, 2)  NOT NULL,
    approval_threshold      NUMERIC(19, 2),
    allowed_merchants       VARCHAR(2000)   NOT NULL DEFAULT '',
    allowed_categories      VARCHAR(2000)   NOT NULL DEFAULT '',
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_agent_credentials_api_key_hash UNIQUE (api_key_hash)
);

CREATE TABLE payment_intents (
    id                  UUID            PRIMARY KEY,
    agent_id            UUID            NOT NULL,
    wallet_id           UUID            NOT NULL,
    merchant_id         VARCHAR(100)    NOT NULL,
    merchant_category   VARCHAR(100)    NOT NULL,
    amount              NUMERIC(19, 2)  NOT NULL,
    currency            VARCHAR(3)      NOT NULL,
    description         VARCHAR(500),
    idempotency_key     VARCHAR(255)    NOT NULL,
    request_fingerprint VARCHAR(64)     NOT NULL,
    status              VARCHAR(30)     NOT NULL,
    decision_reason     VARCHAR(100),
    processor_reference VARCHAR(100),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_payment_intents_agent_idempotency UNIQUE (agent_id, idempotency_key)
);

CREATE INDEX idx_payment_intents_agent_created ON payment_intents (agent_id, created_at);
