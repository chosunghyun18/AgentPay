package com.agentpay.agent.domain;

import java.util.Optional;

/** 에이전트 자격 증명 저장소 포트. */
public interface AgentCredentialRepository {

    AgentCredential save(AgentCredential credential);

    Optional<AgentCredential> findById(AgentId id);

    Optional<AgentCredential> findByApiKeyHash(ApiKeyHash hash);
}
