# 주문 생성 시퀀스 상세

## 1. 개요

- 목적: 주문 생성의 신뢰성(메시지 유실·중복 방지, 멱등성)과 비동기 통합 흐름(Outbox → Kafka → 소비자)을 설명합니다.
- Transactional Outbox와 퍼블리셔의 lease·재시도·멱등성은 분산 환경에서 주문 처리의 신뢰성을 보장하는 핵심 메커니즘입니다.
- 핵심 패턴: Transactional Outbox, Lease, Idempotency

## 2. 단계별 시퀀스

다이어그램

![Order create sequence](order-create-sequence.png)

### 1) 클라이언트 요청

- `POST /orders` (헤더: `Idempotency-Key`) 호출
- 입력 검증 및 상품/가격/재고 조회(필요 시 Redis 캐시)

### 2) 주문 생성 트랜잭션

- Order를 DB에 저장(상태: `PENDING`/`CREATED`)
- Reservation·Payment 이벤트를 Transactional Outbox에 INSERT
- 트랜잭션 커밋 시 Order와 Outbox가 함께 영구화

### 3) 응답 반환

- 트랜잭션 성공 시 HTTP 201과 `orderId` 반환
- 동일 `Idempotency-Key` 재요청 시 기존 결과 반환(멱등성)

### 4) Outbox 퍼블리셔

- PENDING 레코드 선점(lease) → Kafka로 발행 → Outbox 상태 갱신(SENT/FAILED)
- Lease로 다중 인스턴스 중복 발행 방지

### 5) Inventory 처리

- `order-reservation` 이벤트 수신 → 재고 선점 시도
- 성공: `InventoryReserved`, 실패: `InventoryFailed` 이벤트 발행

### 6) 주문 상태 보완

- Inventory/Payment 결과를 구독해 주문 상태 업데이트(예: RESERVED → CONFIRMED/CANCELLED)
- 이벤트 `eventId`로 소비자 측 멱등 처리

### 7) Payment 처리

- 결제 승인/취소 처리 → `PaymentApproved`/`PaymentFailed` 이벤트 발행

### 8) 보상(Compensation)

- 실패 시 재고 롤백·주문 취소·환불 등 보상 이벤트로 처리
- 보상도 Outbox 패턴으로 발행

### 9) 캐시/조회 반영

- Product 서비스가 재고 이벤트를 수신해 Redis 캐시 갱신

### 10) 실패/복구

- 퍼블리셔 중단: PENDING은 재시도 보장
- 브로커 장애: 재시도·백오프·모니터링
- 이벤트 중복: `eventId` 기반 멱등 처리
