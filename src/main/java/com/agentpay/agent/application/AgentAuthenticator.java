package com.agentpay.agent.application;

import com.agentpay.agent.domain.AgentCredential;
import com.agentpay.agent.domain.AgentCredentialRepository;
import com.agentpay.agent.domain.ApiKeyHash;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** API 키 원문 → 활성 에이전트 조회. 실패 사유는 외부에 구분해서 노출하지 않는다. */
@Service
public class AgentAuthenticator {

    private final AgentCredentialRepository repository;

    public AgentAuthenticator(AgentCredentialRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<AgentCredential> authenticate(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            return Optional.empty();
        }
        return repository.findByApiKeyHash(ApiKeyHash.of(rawApiKey))
                .filter(AgentCredential::isActive);
    }
}
