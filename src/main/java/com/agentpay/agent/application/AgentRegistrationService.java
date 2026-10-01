package com.agentpay.agent.application;

import com.agentpay.agent.domain.AgentCredential;
import com.agentpay.agent.domain.AgentCredentialRepository;
import com.agentpay.agent.domain.ApiKey;
import com.agentpay.agent.domain.SpendingPolicy;
import com.agentpay.wallet.domain.WalletId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class AgentRegistrationService {

    private final AgentCredentialRepository repository;
    private final Clock clock;

    public AgentRegistrationService(AgentCredentialRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** 에이전트를 등록하고 API 키 원문을 반환한다. 원문은 이 응답에서만 확인 가능하다. */
    @Transactional
    public Registered register(WalletId walletId, String name, SpendingPolicy policy) {
        ApiKey apiKey = ApiKey.generate();
        AgentCredential credential = repository.save(
                AgentCredential.create(walletId, name, apiKey.hash(), policy, clock.instant()));
        return new Registered(credential, apiKey);
    }

    public record Registered(AgentCredential credential, ApiKey apiKey) {
    }
}
