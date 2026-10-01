package com.agentpay.agent.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentCredentialJpaRepository extends JpaRepository<AgentCredentialJpaEntity, UUID> {

    Optional<AgentCredentialJpaEntity> findByApiKeyHash(String apiKeyHash);
}
