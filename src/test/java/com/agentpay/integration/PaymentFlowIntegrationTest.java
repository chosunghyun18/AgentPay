package com.agentpay.integration;

import com.agentpay.agent.application.AgentRegistrationService;
import com.agentpay.agent.domain.SpendingPolicy;
import com.agentpay.global.domain.CurrencyCode;
import com.agentpay.global.domain.Money;
import com.agentpay.wallet.application.WalletService;
import com.agentpay.wallet.domain.Wallet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 전체 흐름 (H2 PostgreSQL 모드 + Flyway 마이그레이션 + JPA 어댑터 + Fake 처리기).
 * Docker 불필요.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentFlowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired WalletService walletService;
    @Autowired AgentRegistrationService registrationService;

    private String apiKey;
    private Wallet wallet;
    private String ownerId;

    @BeforeEach
    void setUp() {
        ownerId = "owner-" + UUID.randomUUID();
        wallet = walletService.open(ownerId, Money.of(100_000, CurrencyCode.KRW));
        SpendingPolicy policy = new SpendingPolicy(Money.of(50_000, CurrencyCode.KRW),
                Money.of(80_000, CurrencyCode.KRW), Money.of(30_000, CurrencyCode.KRW), Set.of(), Set.of("books"));
        apiKey = registrationService.register(wallet.id(), "it-agent", policy).apiKey().raw();
    }

    private JsonNode createIntent(String key, long amount) throws Exception {
        String body = """
                {"merchantId":"m_yes24","merchantCategory":"books","amount":%d,"currency":"KRW"}
                """.formatted(amount);
        String json = mockMvc.perform(post("/v1/payment-intents")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    @Test
    @DisplayName("가승인 → 매입 → 지갑 차감")
    void authorizeCaptureAndDebit() throws Exception {
        JsonNode created = createIntent("it-1", 12_000);
        String id = created.get("id").asText();
        assertThat(created.get("status").asText()).isEqualTo("AUTHORIZED");

        mockMvc.perform(post("/v1/payment-intents/{id}/capture", id).header("Authorization", "Bearer " + apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CAPTURED"));

        mockMvc.perform(get("/v1/wallets/{id}", wallet.id().toString()).header("X-Owner-Id", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(88000));
    }

    @Test
    @DisplayName("임계 이상 → 승인 대기 → 소유자 승인 → 조회")
    void humanApproval() throws Exception {
        JsonNode created = createIntent("it-2", 40_000);
        String id = created.get("id").asText();
        assertThat(created.get("status").asText()).isEqualTo("PENDING_APPROVAL");

        mockMvc.perform(post("/v1/payment-intents/{id}/approve", id).header("X-Owner-Id", "intruder"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/v1/payment-intents/{id}/approve", id).header("X-Owner-Id", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));

        mockMvc.perform(get("/v1/payment-intents/{id}", id).header("Authorization", "Bearer " + apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }

    @Test
    @DisplayName("멱등 재요청은 같은 id, 다른 내용은 409")
    void idempotencyAgainstDatabase() throws Exception {
        JsonNode first = createIntent("it-3", 5_000);
        JsonNode replay = createIntent("it-3", 5_000);
        assertThat(replay.get("id").asText()).isEqualTo(first.get("id").asText());

        JsonNode conflict = createIntent("it-3", 6_000);
        assertThat(conflict.get("code").asText()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
    }

    @Test
    @DisplayName("허용 목록 밖 가맹점은 REJECTED로 기록된다")
    void disallowedMerchantIsRecordedAsRejected() throws Exception {
        mockMvc.perform(post("/v1/payment-intents")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Idempotency-Key", "it-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantId\":\"m_casino\",\"merchantCategory\":\"gambling\",\"amount\":1000,\"currency\":\"KRW\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.decisionReason").value("MERCHANT_NOT_ALLOWED"));
    }
}
