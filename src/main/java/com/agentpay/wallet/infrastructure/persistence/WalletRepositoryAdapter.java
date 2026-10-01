package com.agentpay.wallet.infrastructure.persistence;

import com.agentpay.global.domain.CurrencyCode;
import com.agentpay.global.domain.Money;
import com.agentpay.wallet.domain.Wallet;
import com.agentpay.wallet.domain.WalletId;
import com.agentpay.wallet.domain.WalletRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class WalletRepositoryAdapter implements WalletRepository {

    private final WalletJpaRepository jpaRepository;

    public WalletRepositoryAdapter(WalletJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Wallet save(Wallet wallet) {
        WalletJpaEntity entity = jpaRepository.findById(wallet.id().value())
                .map(existing -> {
                    existing.updateBalance(wallet.balance().amount());
                    return existing;
                })
                .orElseGet(() -> new WalletJpaEntity(
                        wallet.id().value(),
                        wallet.ownerId(),
                        wallet.balance().currency().name(),
                        wallet.balance().amount(),
                        wallet.createdAt()));
        jpaRepository.save(entity);
        return wallet;
    }

    @Override
    public Optional<Wallet> findById(WalletId id) {
        return jpaRepository.findById(id.value()).map(WalletRepositoryAdapter::toDomain);
    }

    private static Wallet toDomain(WalletJpaEntity e) {
        return Wallet.reconstitute(
                new WalletId(e.getId()),
                e.getOwnerId(),
                new Money(e.getBalance(), CurrencyCode.valueOf(e.getCurrency())),
                e.getCreatedAt());
    }
}
