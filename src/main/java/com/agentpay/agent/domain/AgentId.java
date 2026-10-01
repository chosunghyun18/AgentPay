package com.agentpay.agent.domain;

import java.util.Objects;
import java.util.UUID;

public record AgentId(UUID value) {

    public AgentId {
        Objects.requireNonNull(value, "value");
    }

    public static AgentId newId() {
        return new AgentId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
