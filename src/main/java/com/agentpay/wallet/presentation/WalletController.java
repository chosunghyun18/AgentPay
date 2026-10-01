package com.agentpay.wallet.presentation;

import com.agentpay.global.web.HumanPrincipal;
import com.agentpay.wallet.application.WalletService;
import com.agentpay.wallet.domain.Wallet;
import com.agentpay.wallet.domain.WalletId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{walletId}")
    public WalletResponse get(@PathVariable String walletId,
                              @RequestHeader(HumanPrincipal.HEADER) String ownerId) {
        Wallet wallet = walletService.getOwned(WalletId.of(walletId), ownerId);
        return new WalletResponse(wallet.id().toString(), wallet.ownerId(),
                wallet.balance().amount(), wallet.balance().currency().name());
    }

    public record WalletResponse(String id, String ownerId, BigDecimal balance, String currency) {
    }
}
