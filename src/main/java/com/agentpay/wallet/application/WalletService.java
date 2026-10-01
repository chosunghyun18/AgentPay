package com.agentpay.wallet.application;

import com.agentpay.global.domain.ForbiddenException;
import com.agentpay.global.domain.Money;
import com.agentpay.global.domain.NotFoundException;
import com.agentpay.wallet.domain.Wallet;
import com.agentpay.wallet.domain.WalletId;
import com.agentpay.wallet.domain.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final Clock clock;

    public WalletService(WalletRepository walletRepository, Clock clock) {
        this.walletRepository = walletRepository;
        this.clock = clock;
    }

    @Transactional
    public Wallet open(String ownerId, Money initialBalance) {
        return walletRepository.save(Wallet.create(ownerId, initialBalance, clock.instant()));
    }

    @Transactional(readOnly = true)
    public Wallet getOwned(WalletId id, String ownerId) {
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Wallet", id));
        if (!wallet.isOwnedBy(ownerId)) {
            throw new ForbiddenException("지갑 소유자가 아닙니다");
        }
        return wallet;
    }
}
