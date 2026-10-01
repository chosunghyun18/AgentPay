# CLAUDE.md — AgentPay

## 프로젝트 개요

**AgentPay**는 AI 에이전트가 사람이 위임한 한도 안에서 안전하게 결제할 수 있게 하는 결제 API 서비스입니다.
사람(지갑 소유자)이 지출 정책을 정하고, 에이전트는 API 키 + Idempotency-Key로 결제를 요청하며,
임계 금액 이상은 사람 승인을 거칩니다.

> **설계 SSOT는 Obsidian `Projects/work/AgentPay/AgentPay Design Spec.md`.**
> 코드와 문서가 충돌하면 **문서가 우선**한다. 설계를 바꾸면 문서를 먼저 고친다.

## 구조

```
src/main/java/com/agentpay/
├── global/    shared kernel(Money, CurrencyCode, DomainException) + 설정/웹 공통
├── wallet/    지갑 (소유자 = 사람)
├── agent/     에이전트 자격 증명 + SpendingPolicy
└── payment/   PaymentIntent 상태 머신, 정책 평가, 결제 처리기 포트
```

각 컨텍스트는 `presentation / application / domain / infrastructure` 4개 서브패키지.

## 기술 스택

Spring Boot 3.3, Java 21, PostgreSQL + Flyway, JPA. 테스트는 JUnit 5 + H2(PostgreSQL 모드) — Docker 불필요.

## 아키텍처 규칙 (헥사고날, dash BE 컨벤션)

```
presentation → application → domain ← infrastructure(adapter)
```

- **domain에 Spring/JPA import 금지** — `DomainPurityTest`가 강제한다
- **Repository/외부 시스템 포트는 domain 소유**, 구현은 infrastructure 어댑터 (`PaymentProcessor` 포함)
- 애그리거트 간 참조는 ID VO로 (`WalletId`, `AgentId`). DB도 FK 제약 없이 UUID 컬럼
- 도메인 팩토리 `create`(신규) / `reconstitute`(복원) 구분. 변경 후 application이 명시적 `save()`
- 금액은 반드시 `Money` (BigDecimal). `double`/`float` 금지, 암묵적 반올림 금지
- 시간은 `Clock` 빈으로만 얻는다 (`Instant.now()` 직접 호출 금지)
- DB 쓰기 유스케이스는 `@Transactional`
- 스키마 변경은 Flyway 마이그레이션 추가로만 (`ddl-auto: validate`)
- 상태 전이는 `PaymentStatus.canTransitionTo`를 통해서만

## 인증 (Phase 0 단순화 — 운영 사용 불가)

- 에이전트: `Authorization: Bearer ap_live_...` → `@AgentAuthenticated` 핸들러에 인터셉터 적용, 키는 SHA-256 해시만 저장
- 사람: `X-Owner-Id` 헤더 스텁. Phase 1에서 Spring Security로 교체

## 명령

```bash
./gradlew test                                  # 전체 테스트 (Docker 불필요)
docker compose up -d && ./gradlew bootRun --args='--spring.profiles.active=local'
```
