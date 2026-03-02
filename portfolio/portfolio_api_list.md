# 서비스별 API 및 이벤트 요약

## 공통
- 로컬 기본 포트
  - product-service: 8080
  - inventory-service: 8081
  - payment-service: 8082
  - order-service: 8083
- 컨테이너 프로파일: SPRING_PROFILES_ACTIVE=docker → `application-docker.yml` 사용

---

## 1) order-service

역할
- 주문 생성(멱등성), 주문 목록 조회
- 주문 저장 시 Transactional Outbox에 이벤트 기록(퍼블리셔가 Kafka로 발행)

REST API
- POST /api/orders
  - Request (JSON)

```json
{
  "idempotencyKey": "<UUID>",
  "userId": "user-123",
  "items": [{"productId":"<UUID>","qty":2,"unitPriceAmount":199900,"unitPriceCurrency":"KRW"}],
  "totalAmount":399800,
  "currency":"KRW"
}
```

  - Response: 201 Created

```json
{ "orderId": "<UUID>" }
```

- GET /api/orders?userId={userId}&page={n}&size={m}
  - Response: 주문 요약 페이지 (orderId, userId, status, totalAmount, currency)

예시

```bash
curl -v -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -d '{"idempotencyKey":"11111111-2222-3333-4444-555555555555","userId":"user-123","items":[{"productId":"9f1c2e6a-...","qty":2,"unitPriceAmount":199900,"unitPriceCurrency":"KRW"}],"totalAmount":399800,"currency":"KRW"}'
```

Kafka / Outbox (요약)
- OutboxRecord 필드: outboxId, orderId, eventType, idempotencyKey, payload, status, lockedBy, lockedUntil, attemptCount, nextAttemptAt
- 발행 예시(개념)

```json
{
  "eventId":"<UUID>",
  "orderId":"<UUID>",
  "eventType":"RESERVATION_CREATE_REQUEST",
  "payload": { "requestItem": [{"productId":"<UUID>","qty":2}] }
}
```

---

## 2) product-service

역할
- 상품 등록(관리자), 전체/단건 조회
- Kafka 재고 이벤트를 수신해 Redis 캐시에 반영

REST API
- POST /api/products
  - Header: Idempotency-Key: <UUID>
  - Request: { "productName": "Acme Widget", "price": 199900, "currency": "KRW" }
  - Response: { "productId": "<UUID>", "productName": "Acme Widget" }

- GET /api/products/search/all
  - Response: { "size": N, "products": [ {productId, productName, price, stock}, ... ] }

- GET /api/products/search/{productId}
  - Response: { productId, productName, stock, price, currency }

예시

```bash
curl -v -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 22222222-3333-4444-5555-666666666666" \
  -d '{"productName":"Acme Widget Small","price":199900,"currency":"KRW"}'
```

Kafka (수신 요약)
- ProductInboundEvent: { eventId, eventType, payload }
- payload 타입: ProductCreatedPayload { productId, stock }, ProductStockIncrementPayload { orderId, productId, qty }, ProductStockDecrementPayload { orderId, productId, qty }

---

## 3) inventory-service

역할
- 재고 예약/확정/해제 처리(이벤트 소비), Reservation·Inventory 관리
- Reservation 관련 Outbound 이벤트 발행

REST API
- 공개 REST 엔드포인트 없음(이벤트 중심 처리)

Kafka(수신)
- ReservationInboundEvent: { eventId, orderId, eventType, payload }
- payload 예: ReservationCreatePayload { requestItem: [{ productId, qty }, ...] }, ReservationConfirmPayload { reservationId }, ReservationReleasePayload { reservationId }
- 처리 흐름: Create → 재고 예약(또는 실패) → ReservationOutboundEvent 발행

Kafka(발행)
- ReservationOutboundEvent 형식으로 발행(토픽은 설정 참조)

---

## 4) payment-service

역할
- 결제 처리(이벤트 기반), 결제 상태 관리
- PaymentProvider 추상화를 통한 실제 결제 시뮬레이션(예: InMemoryPaymentProvider)

REST API
- 공개 REST 엔드포인트 없음(이벤트 중심 처리)

결제 프로바이더(요약)
- createPayment, commitPayment, releasePayment (비동기 CompletableFuture)
- 멱등성: 이벤트 ID 기반으로 중복 처리 방지

Kafka
- 수신: PaymentInboundEvent (결제 요청/승인/취소 등)
- 발행: PaymentOutboundEvent (결과/상태)