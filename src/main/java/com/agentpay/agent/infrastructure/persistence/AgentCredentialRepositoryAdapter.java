package com.agentpay.agent.infrastructure.persistence;

import com.agentpay.agent.domain.AgentCredential;
import com.agentpay.agent.domain.AgentCredentialRepository;
import com.agentpay.agent.domain.AgentId;
import com.agentpay.agent.domain.AgentStatus;
import com.agentpay.agent.domain.ApiKeyHash;
import com.agentpay.agent.domain.SpendingPolicy;
import com.agentpay.global.domain.CurrencyCode;
import com.agentpay.global.domain.Money;
import com.agentpay.wallet.domain.WalletId;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Repository
public class AgentCredentialRepositoryAdapter implements AgentCredentialRepository {

    private final AgentCredentialJpaRepository jpaRepository;

    public AgentCredentialRepositoryAdapter(AgentCredentialJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AgentCredential save(AgentCredential c) {
        SpendingPolicy p = c.policy();
        jpaRepository.save(new AgentCredentialJpaEntity(
                c.id().value(),
                c.walletId().value(),
                c.name(),
                c.apiKeyHash().value(),
                c.status().name(),
                p.currency().name(),
                p.perTransactionLimit().amount(),
                p.dailyLimit().amount(),
                p.approvalThreshold() == null ? null : p.approvalThreshold().amount(),
                join(p.allowedMerchants()),
                join(p.allowedCategories()),
                c.createdAt()));
        return c;
    }

    @Override
    public Optional<AgentCredential> findById(AgentId id) {
        return jpaRepository.findById(id.value()).map(AgentCredentialRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<AgentCredential> findByApiKeyHash(ApiKeyHash hash) {
        return jpaRepository.findByApiKeyHash(hash.value()).map(AgentCredentialRepositoryAdapter::toDomain);
    }

    private static AgentCredential toDomain(AgentCredentialJpaEntity e) {
        CurrencyCode currency = CurrencyCode.valueOf(e.getPolicyCurrency());
        SpendingPolicy policy = new SpendingPolicy(
                new Money(e.getPerTransactionLimit(), currency),
                new Money(e.getDailyLimit(), currency),
                e.getApprovalThreshold() == null ? null : new Money(e.getApprovalThreshold(), currency),
                split(e.getAllowedMerchants()),
                split(e.getAllowedCategories()));
        return AgentCredential.reconstitute(
                new AgentId(e.getId()),
                new WalletId(e.getWalletId()),
                e.getName(),
                new ApiKeyHash(e.getApiKeyHash()),
                AgentStatus.valueOf(e.getStatus()),
                policy,
                e.getCreatedAt());
    }

    private static String join(Set<String> values) {
        return String.join(",", new TreeSet<>(values));
    }

    private static Set<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
