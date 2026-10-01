package com.agentpay.wallet.domain;

import java.util.Optional;

/** 지갑 저장소 포트 (domain 소유, infrastructure 어댑터가 구현). */
public interface WalletRepository {

    Wallet save(Wallet wallet);

    Optional<Wallet> findById(WalletId id);
}
