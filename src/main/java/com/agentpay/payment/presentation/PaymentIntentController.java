package com.agentpay.payment.presentation;

import com.agentpay.agent.domain.AgentCredential;
import com.agentpay.agent.presentation.AgentAuthInterceptor;
import com.agentpay.agent.presentation.AgentAuthenticated;
import com.agentpay.global.domain.CurrencyCode;
import com.agentpay.global.domain.Money;
import com.agentpay.global.web.HumanPrincipal;
import com.agentpay.payment.application.CreatePaymentIntentCommand;
import com.agentpay.payment.application.CreatePaymentIntentResult;
import com.agentpay.payment.application.PaymentIntentService;
import com.agentpay.payment.domain.IdempotencyKey;
import com.agentpay.payment.domain.Merchant;
import com.agentpay.payment.domain.PaymentIntentId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 의도 API.
 * <ul>
 *   <li>에이전트용 (API 키): create / get / capture</li>
 *   <li>사람용 (Phase 0 스텁 헤더 X-Owner-Id): approve / reject</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/payment-intents")
public class PaymentIntentController {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotent-Replayed";

    private final PaymentIntentService service;

    public PaymentIntentController(PaymentIntentService service) {
        this.service = service;
    }

    @AgentAuthenticated
    @PostMapping
    public ResponseEntity<PaymentIntentResponse> create(
            @RequestAttribute(AgentAuthInterceptor.ATTRIBUTE) AgentCredential agent,
            @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Valid @RequestBody CreatePaymentIntentRequest request) {
        CreatePaymentIntentResult result = service.create(new CreatePaymentIntentCommand(
                agent,
                new IdempotencyKey(idempotencyKey),
                new Merchant(request.merchantId(), request.merchantCategory()),
                new Money(request.amount(), CurrencyCode.valueOf(request.currency())),
                request.description()));
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header(REPLAYED_HEADER, String.valueOf(result.replayed()))
                .body(PaymentIntentResponse.from(result.intent()));
    }

    @AgentAuthenticated
    @GetMapping("/{id}")
    public PaymentIntentResponse get(@RequestAttribute(AgentAuthInterceptor.ATTRIBUTE) AgentCredential agent,
                                     @PathVariable String id) {
        return PaymentIntentResponse.from(service.getForAgent(PaymentIntentId.of(id), agent.id()));
    }

    @AgentAuthenticated
    @PostMapping("/{id}/capture")
    public PaymentIntentResponse capture(@RequestAttribute(AgentAuthInterceptor.ATTRIBUTE) AgentCredential agent,
                                         @PathVariable String id) {
        return PaymentIntentResponse.from(service.capture(PaymentIntentId.of(id), agent.id()));
    }

    @PostMapping("/{id}/approve")
    public PaymentIntentResponse approve(@RequestHeader(HumanPrincipal.HEADER) String ownerId,
                                         @PathVariable String id) {
        return PaymentIntentResponse.from(service.approve(PaymentIntentId.of(id), ownerId));
    }

    @PostMapping("/{id}/reject")
    public PaymentIntentResponse reject(@RequestHeader(HumanPrincipal.HEADER) String ownerId,
                                        @PathVariable String id) {
        return PaymentIntentResponse.from(service.reject(PaymentIntentId.of(id), ownerId));
    }
}
