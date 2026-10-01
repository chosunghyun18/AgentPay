# AgentPay

AI 에이전트가 **사람이 위임한 한도 안에서** 쉽게 결제할 수 있게 하는 결제 API 서비스.

- 사람(지갑 소유자)이 에이전트별 **지출 정책**(건당/일일 한도, 가맹점·카테고리 허용 목록, 승인 임계)을 정한다
- 에이전트는 API 키 + `Idempotency-Key`로 결제 의도(PaymentIntent)를 만든다 — 재시도해도 이중 결제가 없다
- 정책 평가 결과에 따라 **즉시 가승인 / 사람 승인 대기 / 거절**
- 실제 결제망(PG/카드/스테이블코인)은 `PaymentProcessor` 포트 뒤에 숨긴다 (현재는 Fake 어댑터)

> 설계 SSOT: Obsidian `Projects/work/AgentPay/AgentPay Design Spec.md` (충돌 시 문서 우선)

## 구조

```
AgentPay/
├── build.gradle / settings.gradle / gradlew     Spring Boot 3.3, Java 21
├── docker-compose.yml                           로컬 PostgreSQL
└── src/
    ├── main/java/com/agentpay/
    │   ├── global/      Money·CurrencyCode(shared kernel), 예외 처리, 설정
    │   ├── wallet/      Wallet 애그리거트 (소유자 = 사람)
    │   ├── agent/       AgentCredential, SpendingPolicy, API 키 인증
    │   └── payment/     PaymentIntent 상태 머신, SpendingPolicyEvaluator, PaymentProcessor 포트
    │       ├── presentation/    REST 컨트롤러·DTO
    │       ├── application/     유스케이스 (PaymentIntentService)
    │       ├── domain/          애그리거트·VO·포트 (프레임워크 의존 0)
    │       └── infrastructure/  JPA 어댑터, FakePaymentProcessorAdapter
    ├── main/resources/db/migration/   Flyway 스키마
    └── test/            단위 테스트 + WebMvc 슬라이스 + H2 통합 테스트
```

## 상태 머신

```
CREATED ─┬─(정책 통과)────────────────▶ AUTHORIZED ──capture──▶ CAPTURED ──refund──▶ REFUNDED
         ├─(승인 임계 이상)─▶ PENDING_APPROVAL ─(사람 승인)─▲
         ├─(정책 위반 / 사람 거절)──────▶ REJECTED
         └─(TTL 경과)──────────────────▶ EXPIRED   (PENDING_APPROVAL·AUTHORIZED에서도)
```

## API (v1)

| Method | Path | 주체 | 설명 |
|---|---|---|---|
| POST | `/v1/payment-intents` | 에이전트 (`Authorization: Bearer`, `Idempotency-Key` 필수) | 결제 의도 생성 + 정책 평가. 신규 201, 멱등 재생 200, 내용 다른 재사용 409 |
| GET | `/v1/payment-intents/{id}` | 에이전트 | 상태 조회 |
| POST | `/v1/payment-intents/{id}/capture` | 에이전트 | 매입 확정 + 지갑 차감 |
| POST | `/v1/payment-intents/{id}/approve` | 사람 (`X-Owner-Id`, Phase 0 스텁) | 승인 대기 건 승인 |
| POST | `/v1/payment-intents/{id}/reject` | 사람 | 승인 대기 건 거절 |
| GET | `/v1/wallets/{id}` | 사람 | 잔액 조회 |

오류 응답은 `{"code": "...", "message": "..."}` 고정 형태 (에이전트가 code로 분기).

## 실행 방법

```bash
# 테스트 (Docker 불필요 — H2 PostgreSQL 모드)
./gradlew test

# 로컬 실행: PostgreSQL + local 프로필(데모 지갑·에이전트 시드, API 키는 로그에 출력)
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'

# 예시 — 로그의 apiKey 사용
curl -X POST localhost:8080/v1/payment-intents \
  -H "Authorization: Bearer ap_live_..." \
  -H "Idempotency-Key: order-1234" \
  -H "Content-Type: application/json" \
  -d '{"merchantId":"m_yes24","merchantCategory":"books","amount":12000,"currency":"KRW","description":"책 구매"}'
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

## 로드맵

| Phase | 내용 | 상태 |
|---|---|---|
| **0** | 스캐폴드: 도메인 모델·상태 머신·정책 평가·멱등성, Fake 처리기, REST API, 테스트 | ✅ 현재 |
| **1** | 사람 인증(Spring Security + OAuth2/JWT 또는 passkey), 에이전트 등록/정책 변경/키 폐기 API, 승인 알림(Slack/푸시), 만료 스케줄러, 지갑 홀드 | 예정 |
| **2** | 실제 PG 연동 (Toss Payments 우선 검토, Stripe) — 웹훅, 대사(reconciliation), outbox | 예정 |
| **3** | **MCP 서버로 노출** — `create_payment_intent` / `get_payment_status` 툴로 에이전트가 직접 호출 | 예정 |
| **4** | 에이전트 커머스 프로토콜 평가·연동 (Stripe ACP, Google AP2, 카드사 agent token, x402) — 설계 문서 참고 | 조사 |
