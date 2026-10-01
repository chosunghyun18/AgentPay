package com.agentpay.payment.presentation;

import com.agentpay.agent.application.AgentAuthenticator;
import com.agentpay.agent.domain.AgentCredential;
import com.agentpay.agent.domain.ApiKey;
import com.agentpay.agent.domain.SpendingPolicy;
import com.agentpay.agent.presentation.AgentAuthInterceptor;
import com.agentpay.global.config.WebConfig;
import com.agentpay.global.domain.CurrencyCode;
import com.agentpay.global.domain.Money;
import com.agentpay.payment.application.CreatePaymentIntentCommand;
import com.agentpay.payment.application.CreatePaymentIntentResult;
import com.agentpay.payment.application.PaymentIntentService;
import com.agentpay.payment.domain.IdempotencyConflictException;
import com.agentpay.payment.domain.IdempotencyKey;
import com.agentpay.payment.domain.Merchant;
import com.agentpay.payment.domain.PaymentIntent;
import com.agentpay.wallet.domain.WalletId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentIntentController.class)
@Import({WebConfig.class, AgentAuthInterceptor.class})
class PaymentIntentControllerTest {

    private static final String API_KEY = ApiKey.generate().raw();
    private static final Instant NOW = Instant.parse("2026-10-01T03:00:00Z");
    private static final String BODY = """
            {"merchantId":"m_yes24","merchantCategory":"books","amount":12000,"currency":"KRW","description":"책"}
            """;

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PaymentIntentService service;

    @MockBean
    AgentAuthenticator authenticator;

    private AgentCredential agent;

    @BeforeEach
    void setUp() {
        SpendingPolicy policy = new SpendingPolicy(Money.of(100_000, CurrencyCode.KRW),
                Money.of(300_000, CurrencyCode.KRW), null, Set.of(), Set.of());
        agent = AgentCredential.create(WalletId.newId(), "agent", new ApiKey(API_KEY).hash(), policy, NOW);
        when(authenticator.authenticate(anyString())).thenReturn(Optional.empty());
        when(authenticator.authenticate(API_KEY)).thenReturn(Optional.of(agent));
    }

    private PaymentIntent authorizedIntent() {
        PaymentIntent intent = PaymentIntent.create(agent.id(), agent.walletId(), new Merchant("m_yes24", "books"),
                Money.of(12_000, CurrencyCode.KRW), "책", new IdempotencyKey("k-1"), NOW, Duration.ofMinutes(30));
        intent.authorize("ref", NOW);
        return intent;
    }

    @Test
    void createReturns201() throws Exception {
        when(service.create(any())).thenReturn(new CreatePaymentIntentResult(authorizedIntent(), false));

        mockMvc.perform(post("/v1/payment-intents")
                        .header("Authorization", "Bearer " + API_KEY)
                        .header("Idempotency-Key", "k-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "false"))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.amount").value(12000))
                .andExpect(jsonPath("$.currency").value("KRW"));

        ArgumentCaptor<CreatePaymentIntentCommand> captor = ArgumentCaptor.forClass(CreatePaymentIntentCommand.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().agent()).isSameAs(agent);
        assertThat(captor.getValue().idempotencyKey().value()).isEqualTo("k-1");
        assertThat(captor.getValue().amount()).isEqualTo(Money.of(12_000, CurrencyCode.KRW));
    }

    @Test
    void replayReturns200() throws Exception {
        when(service.create(any())).thenReturn(new CreatePaymentIntentResult(authorizedIntent(), true));

        mockMvc.perform(post("/v1/payment-intents")
                        .header("Authorization", "Bearer " + API_KEY)
                        .header("Idempotency-Key", "k-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"));
    }

    @Test
    void missingApiKeyReturns401() throws Exception {
        mockMvc.perform(post("/v1/payment-intents")
                        .header("Idempotency-Key", "k-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verify(service, never()).create(any());
    }

    @Test
    void invalidApiKeyReturns401() throws Exception {
        mockMvc.perform(post("/v1/payment-intents")
                        .header("Authorization", "Bearer ap_live_wrongwrongwrongwrongwrongwrongwrong")
                        .header("Idempotency-Key", "k-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingIdempotencyKeyReturns400() throws Exception {
        mockMvc.perform(post("/v1/payment-intents")
                        .header("Authorization", "Bearer " + API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest());
        verify(service, never()).create(any());
    }

    @Test
    void invalidBodyReturns400() throws Exception {
        mockMvc.perform(post("/v1/payment-intents")
                        .header("Authorization", "Bearer " + API_KEY)
                        .header("Idempotency-Key", "k-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantId\":\"m\",\"merchantCategory\":\"c\",\"amount\":-1,\"currency\":\"KRW\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void idempotencyConflictReturns409() throws Exception {
        when(service.create(any())).thenThrow(new IdempotencyConflictException(new IdempotencyKey("k-1")));

        mockMvc.perform(post("/v1/payment-intents")
                        .header("Authorization", "Bearer " + API_KEY)
                        .header("Idempotency-Key", "k-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
    }
}
