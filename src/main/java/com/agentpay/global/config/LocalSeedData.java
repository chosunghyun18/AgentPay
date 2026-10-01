package com.agentpay.global.config;

import com.agentpay.agent.application.AgentRegistrationService;
import com.agentpay.agent.domain.SpendingPolicy;
import com.agentpay.global.domain.CurrencyCode;
import com.agentpay.global.domain.Money;
import com.agentpay.wallet.application.WalletService;
import com.agentpay.wallet.domain.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Set;

/**
 * {@code local} 프로필 전용 데모 데이터: 지갑 1개 + 에이전트 1개를 만들고 API 키를 로그로 출력한다.
 * 에이전트 등록 API는 사람 인증이 갖춰지는 Phase 1에서 공개한다.
 */
@Configuration
@Profile("local")
public class LocalSeedData {

    private static final Logger log = LoggerFactory.getLogger(LocalSeedData.class);

    @Bean
    public ApplicationRunner seed(WalletService walletService, AgentRegistrationService registrationService) {
        return args -> {
            Wallet wallet = walletService.open("demo-owner", Money.of(500_000, CurrencyCode.KRW));
            SpendingPolicy policy = new SpendingPolicy(
                    Money.of(100_000, CurrencyCode.KRW),   // 건당 10만원
                    Money.of(300_000, CurrencyCode.KRW),   // 일 30만원
                    Money.of(50_000, CurrencyCode.KRW),    // 5만원 이상은 사람 승인
                    Set.of(),
                    Set.of("books", "cloud", "food"));
            var registered = registrationService.register(wallet.id(), "demo-agent", policy);
            log.info("[local-seed] walletId={} ownerId=demo-owner agentId={} apiKey={}",
                    wallet.id(), registered.credential().id(), registered.apiKey().raw());
        };
    }
}
