# CreateOrderUseCase 실행 시나리오 문서
---

## 1. 고수준 흐름 요약
1. 클라이언트 → `CreateOrderUseCase.create(command)`
2. 비트랜잭션 멱등성 체크: `orderRepository.findByUserIdAndIdempotencyKey(...)` (트랜잭션 외)
   - 이미 주문이 존재하면 기존 주문 ID를 반환하고 종료
3. 상품 가격 스냅샷 일괄 조회: `productClient.getPriceSnapshots(productIds)` (외부 호출, 트랜잭션 외)
4. 주문 아이템 빌드: `OrderItem.create(...)` (트랜잭션 외)
5. ReserveInventory 페이로드(JSON) 생성
6. 트랜잭션 A: 주문 저장(`orderRepository.save`) + 아웃박스 저장(`orderOutboxRepository.save`)
   - DataIntegrityViolationException 발생 시 기존 주문 조회 후 반환(멱등성 보장)
7. 트랜잭션 B: `OrderSaga.create` 및 상태 변경 저장, 주문 상태 `markPaying()` 변경 후 저장
8. 최종 반환: `Result(saved.id)`

---

## 2. 주요 시나리오
아래 각 시나리오는 원인, 동작, 결과 및 테스트 관점의 확인 포인트를 포함한다.

### 가. 정상 흐름 (Happy Case)
- 조건: idempotency 미발견, productClient 정상 응답, DB 저장 성공
- 동작: 주문 생성 → 아웃박스 저장 → saga 생성 및 주문 상태 `markPaying`
- 결과: `Result(orderId)` 반환
- 검증 포인트: Order 저장 확인, Outbox 저장(종류 `RESERVE_INVENTORY_REQUEST`) 확인, OrderSaga 상태 확인

### 나. 멱등성으로 이미 주문 존재
- 원인: 동일 userId + idempotencyKey로 이미 주문 존재
- 동작: 초기 멱등성 체크에서 기존 주문 ID 반환
- 결과: 기존 `orderId` 반환
- 검증 포인트: 저장/아웃박스/후처리 호출이 발생하지 않았는지 확인

### 다. 상품 스냅샷 조회 실패 (외부 호출 실패)
- 원인: product service 네트워크/오류
- 동작: `productClient.getPriceSnapshots`에서 예외 발생 → `OrderException.InvalidCommand("failed to fetch product price snapshots")` throw
- 결과: 예외 발생(호출자 처리 필요)
- 검증 포인트: 예외 타입/메시지 확인

### 라. 요청 상품 일부 미존재
- 원인: 조회된 스냅샷에 일부 productId 없음
- 동작: `OrderException.InvalidCommand("product not found: {id}")` throw
- 결과: 예외 발생
- 검증 포인트: 예외 발생 위치(아이템 빌드 단계)

### 마. 트랜잭션 A에서 저장 중 무결성 위반(동시성)
- 원인: 동시성으로 동일 idempotencyKey 레코드 동시 생성 시도
- 동작: `orderRepository.save`에서 `DataIntegrityViolationException` 발생 → 기존 주문 조회 후 반환
- 결과: 기존 주문 반환(멱등성 보장)
- 검증 포인트: save 예외 시 기존 주문을 조회해 반환하는 분기 검증

### 바. 아웃박스 저장 실패
- 원인: Outbox DB 문제
- 동작: `orderOutboxRepository.save` 실패 → 예외 전파 → 트랜잭션 A 롤백
- 결과: 주문도 롤백, 호출자에게 예외 전파
- 검증 포인트: 트랜잭션 롤백 여부(영속화된 주문 없음)

### 사. 후처리 트랜잭션(트랜잭션 B)에서 주문 조회 실패
- 원인: 예측 불가(설계상 발생하지 않음)
- 동작: `orderRepository.findById(saved.id)` null → `OrderException.InvalidCommand("order not found: {id}")`
- 결과: 예외 발생(운영상 심각)
- 검증 포인트: 예외 발생 및 로그/알림 트리거

---

## 3. 트랜잭션/경계 요약
- 비트랜잭션(외부 호출 포함): 멱등성 초기 체크, 외부 product snapshot 호출, 도메인 오브젝트 조립
- 트랜잭션 A: 주문 저장 + 아웃박스 이벤트 저장 (원자성 필요)
- 트랜잭션 B: 사가 생성/상태 변경 및 주문 상태 전이