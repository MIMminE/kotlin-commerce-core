# Kotlin Commerce Core

Kotlin과 Spring Boot로 구현한 이벤트 기반 커머스 마이크로서비스 프로젝트입니다. 주문 생성, 재고 예약, 결제 처리, 상품 재고 동기화까지 이어지는 흐름을 Kafka 이벤트로 연결하고, Transactional Outbox, Lease, Idempotency, Redis 캐시를 적용해 분산 환경에서 자주 발생하는 중복 처리와 메시지 유실 문제를 다룹니다.

> 이직 포트폴리오 관점에서는 “단순 CRUD 백엔드”가 아니라, 주문 도메인에서 필요한 신뢰성 패턴을 작은 MSA로 직접 구현한 프로젝트입니다.

<p align="center">
  <img src="portfolio/web-client-preview.png" alt="Commerce client preview" width="900" />
</p>

## Highlights

- **주문-재고-결제-상품 서비스 분리**: `order-service`, `inventory-service`, `payment-service`, `product-service`를 독립 서비스로 구성
- **Transactional Outbox**: 주문 저장과 이벤트 발행 요청을 같은 DB 트랜잭션에 기록해 메시지 유실 위험 감소
- **Lease 기반 Publisher**: `SKIP LOCKED`와 lease owner/lease until을 사용해 다중 퍼블리셔 환경에서 중복 발행 방지
- **Idempotency 처리**: 주문 생성과 상품 등록 요청에 멱등성 키를 적용해 재시도 상황을 안전하게 처리
- **Kafka 이벤트 흐름**: 서비스 간 직접 강결합 대신 이벤트 기반으로 재고 예약, 결제, 상품 재고 반영 수행
- **Redis 캐시**: 상품 조회와 재고 표시를 빠르게 제공하기 위한 캐시 계층 구성
- **웹 클라이언트 추가**: 백엔드 흐름을 브라우저에서 확인할 수 있도록 Vite 기반 클라이언트 화면 제공

## Tech Stack

| 영역 | 사용 기술 |
| --- | --- |
| Language | Kotlin |
| Backend | Spring Boot, Spring Data JPA |
| Messaging | Kafka, Zookeeper |
| Database | PostgreSQL |
| Cache | Redis |
| Test | JUnit, Mockito, Testcontainers |
| Infra | Docker Compose, GitHub Actions |
| Web Client | Vite, Vanilla JavaScript |

## System Overview

<p align="center">
  <img src="portfolio/system-overview.svg" alt="Commerce MSA system overview" width="900" />
</p>

## Web Client

포트폴리오 확인자가 백엔드 API와 이벤트 흐름을 빠르게 확인할 수 있도록 웹 클라이언트를 추가했습니다. 상품 조회, 상품 등록, 주문 생성, 최근 주문 조회를 한 화면에서 실행할 수 있습니다.

```bash
# 1. 백엔드 전체 스택 실행
docker compose up -d --build

# 2. 웹 클라이언트 실행
cd demo-ui
npm install
npm run dev
```

- UI: `http://localhost:5183`
- Product API: `http://localhost:8080`
- Order API: `http://localhost:8083`
- 개발용 API Key: `X-API-Key: dev-api-key-5678`

## UI Planning

이 UI는 프론트엔드 자체를 과하게 보여주기보다, 백엔드 포트폴리오를 보는 사람이 주문 흐름을 직접 눌러볼 수 있게 만드는 데 초점을 맞췄습니다. 그래서 랜딩 페이지나 소개 문구를 크게 두지 않고, 첫 화면에서 바로 상품 탐색과 주문 액션이 보이도록 구성했습니다.

<p align="center">
  <img src="portfolio/ui-main.png" alt="Commerce client main screen" width="520" />
</p>

### 1. 첫 화면은 상품 탐색 중심

- 상단에는 브랜드, 검색, 주요 이동 메뉴, 서비스 상태만 배치했습니다.
- 큰 프로모션 배너는 제거하고 카테고리 필터와 상품 목록을 바로 보여줘 실제 클라이언트 화면에 가깝게 구성했습니다.
- 색상은 무채색 기반으로 낮추고, 버튼과 선택 상태만 진한 네이비로 처리해 포트폴리오 문서 안에서도 튀지 않게 맞췄습니다.

<p align="center">
  <img src="portfolio/ui-catalog.png" alt="Product catalog UI" width="520" />
</p>

### 2. 상품 카드는 재고와 주문 액션을 빠르게 확인

- 상품명, 카테고리, 가격, 재고, 담기 버튼을 한 카드 안에 압축했습니다.
- 실제 상품 이미지가 없는 백엔드 프로젝트 특성을 고려해, 과한 이미지 대신 단순한 상품 썸네일 스타일을 사용했습니다.
- 카드 높이를 낮춰 한 화면에서 여러 상품을 비교할 수 있게 했습니다.

<p align="center">
  <img src="portfolio/ui-checkout.png" alt="Checkout and recent orders UI" width="520" />
</p>

### 3. 주문 생성과 결과 확인을 같은 흐름에 배치

- 주문 폼은 고객 ID, 상품, 수량만 입력하면 바로 요청할 수 있게 최소화했습니다.
- 결제 예정 금액을 즉시 계산해 주문 요청 전에 확인할 수 있게 했습니다.
- 최근 주문 목록을 바로 아래에 두어 `POST /api/orders` 이후 `GET /api/orders`로 상태가 이어지는 흐름을 확인할 수 있습니다.

## Service Responsibilities

| 서비스 | 책임 |
| --- | --- |
| `order-service` | 주문 생성, 주문 조회, 멱등성 처리, Outbox 이벤트 기록 |
| `inventory-service` | 재고 예약, 확정, 해제 처리 및 예약 결과 이벤트 발행 |
| `payment-service` | 결제 생성, 승인, 해제 흐름 처리 및 결제 결과 이벤트 발행 |
| `product-service` | 상품 등록/조회, 재고 이벤트 수신, Redis 캐시 갱신 |

## Order Flow

주문 생성 요청은 즉시 모든 서비스를 동기 호출하지 않고, 주문 저장 후 Outbox에 이벤트를 기록합니다. 이후 Publisher가 Kafka로 이벤트를 발행하고, 재고/결제/상품 서비스가 각자 이벤트를 소비해 상태를 갱신합니다.

<p align="center">
  <img src="portfolio/order-flow.svg" alt="Order event flow" width="900" />
</p>

1. 클라이언트가 `POST /api/orders`로 주문 생성 요청
2. `order-service`가 주문과 Outbox 레코드를 같은 트랜잭션에 저장
3. Outbox Publisher가 `PENDING` 이벤트를 lease로 선점
4. Kafka에 재고 예약 요청 이벤트 발행
5. `inventory-service`가 재고 예약 처리 후 결과 이벤트 발행
6. `payment-service`가 결제 처리 후 결과 이벤트 발행
7. `product-service`가 재고 관련 이벤트를 수신해 Redis 캐시 갱신

## Transactional Outbox

Outbox는 이 프로젝트의 핵심 구현 포인트입니다. 주문 DB 트랜잭션과 Kafka 발행을 직접 하나의 원자적 작업으로 묶을 수 없기 때문에, 우선 DB에 발행할 이벤트를 기록하고 별도 Publisher가 안전하게 발행합니다.

<p align="center">
  <img src="portfolio/outbox-flow.svg" alt="Transactional outbox flow" width="900" />
</p>

### Publisher Claim SQL

```sql
WITH cte AS (
    SELECT id
    FROM outbox
    WHERE status = 'PENDING'
      AND (lease_until IS NULL OR lease_until < now())
    ORDER BY created_at
    LIMIT 50
    FOR UPDATE SKIP LOCKED
)
UPDATE outbox
SET lease_owner = 'publisher-1',
    lease_until = now() + interval '30 seconds',
    status = 'SENDING'
WHERE id IN (SELECT id FROM cte)
RETURNING id, event_id, payload;
```

### Why It Matters

- DB 저장은 성공했지만 Kafka 발행이 실패하는 케이스를 복구 가능하게 만듭니다.
- 여러 Publisher가 동시에 실행되어도 같은 Outbox 레코드를 중복 처리하지 않도록 lease로 보호합니다.
- 실패 횟수, 재시도 시각, 마지막 에러를 기록해 운영 관점의 추적성을 확보합니다.

## API Summary

### Order

```http
POST /api/orders
GET /api/orders?userId={userId}&page={page}&size={size}
```

```json
{
  "idempotencyKey": "9a38df8e-8f42-4b21-94db-3debc5c7f621",
  "userId": "user-123",
  "items": [
    {
      "productId": "product-id",
      "qty": 2,
      "unitPriceAmount": 199900,
      "unitPriceCurrency": "KRW"
    }
  ],
  "totalAmount": 399800,
  "currency": "KRW"
}
```

### Product

```http
POST /api/products
GET /api/products/search/all
GET /api/products/search/{productId}
```

상품 등록은 `Idempotency-Key` 헤더를 사용하고, 로컬 실행 환경에서는 `X-API-Key` 헤더가 필요합니다.

## Local Run

```bash
docker compose up -d --build
```

서비스 포트는 아래와 같습니다.

| 서비스 | 포트 |
| --- | --- |
| product-service | `8080` |
| inventory-service | `8081` |
| payment-service | `8082` |
| order-service | `8083` |
| web client | `5183` |

## CI

GitHub Actions에서 각 서비스의 테스트를 실행하고 테스트 리포트를 업로드합니다.

```text
.github/workflows/ci.yml
```

최근 정리에서 테스트 실패를 무시하던 흐름을 제거해, 서비스 테스트 실패가 CI 결과에 정확히 반영되도록 수정했습니다.

## Portfolio Notes

이 프로젝트에서 어필하고 싶은 지점은 다음과 같습니다.

- 분산 트랜잭션을 단순화하기 위해 Outbox와 이벤트 기반 흐름을 선택한 점
- lease와 `SKIP LOCKED`를 사용해 Publisher 병렬 처리 안정성을 고려한 점
- 멱등성 키로 클라이언트 재시도와 중복 요청을 다룬 점
- Redis 캐시를 상품 조회 흐름에 연결해 읽기 성능을 고려한 점
- Docker Compose와 웹 클라이언트로 면접자가 로컬에서 빠르게 흐름을 확인할 수 있게 만든 점

## Repository Structure

```text
.
├── demo-ui/              # 포트폴리오 확인용 웹 클라이언트
├── inventory-service/    # 재고 예약/확정/해제 이벤트 처리
├── order-service/        # 주문 생성, 멱등성, Outbox 기록
├── payment-service/      # 결제 이벤트 처리
├── product-service/      # 상품 API, 재고 이벤트 수신, Redis 캐시
├── portfolio/            # README용 스크린샷 및 아키텍처 이미지
└── docker-compose.yml
```
