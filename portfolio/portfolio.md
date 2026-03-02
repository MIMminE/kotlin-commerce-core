# kotlin-commerce-core — 개요

Kotlin + Spring Boot로 구현한 이벤트 기반 마이크로서비스(Orders / Inventory / Payment / Product). Transactional Outbox, Lease,
Idempotency, Redis 캐시 등 실무 신뢰성 패턴을 적용한 엔드투엔드 커머스 샘플입니다.

## 핵심 포인트

- 목표: 분산 환경에서 메시지 유실·중복·동시성 문제를 실무 패턴으로 해결
- 패턴: Transactional Outbox, Lease(Claim-and-Lock), Idempotency, Event-Driven Integration
- 모듈: `order-service`, `inventory-service`, `payment-service`, `product-service`

## 1) 프로젝트 목표

- 주문 → 재고 → 결제의 엔드투엔드 이벤트 흐름 구현
- 트랜잭션·메시지 원자성(Outbox) 및 다중 인스턴스 중복 방지(Lease)
- 조회 성능 개선을 위한 Redis 캐시와 통합 테스트 자동화(TestContainers)

## 2) 주 시나리오

- 클라이언트가 `POST /orders`(헤더: `Idempotency-Key`) 호출
- 주문 저장과 동시에 Transactional Outbox에 이벤트 기록 → 201 응답
- Outbox 퍼블리셔가 레코드 선점(lease) 후 Kafka로 발행
- Inventory/Payment가 이벤트를 소비해 처리 → 주문 상태 최종 반영
- Product 서비스는 재고 이벤트로 Redis 캐시 갱신

## 3) 아키텍처 요약

- 책임 분리
    - `order-service`: 주문 도메인, Outbox 기록
    - `inventory-service`: 재고 예약/확정/해제
    - `payment-service`: 결제 처리
    - `product-service`: 상품 조회 + Redis 캐시
- 통합: Kafka 토픽 기반 이벤트 통신 (공통 메타 + payload)
- 신뢰성: Outbox, Lease, 멱등성(eventId / Idempotency-Key)

## 4) 기술 스택

- Kotlin, Spring Boot, Spring Data JPA, Spring Kafka
- PostgreSQL, Redis, Kafka
- JUnit, Mockito, TestContainers

## 5) 빠른 실행

- 로컬 인프라: `docker-compose.yml`(리포지터리 루트)을 사용해 아래를 함께 기동합니다.
    - Zookeeper, Kafka
    - Redis
    - 서비스별 Postgres 인스턴스(호스트 매핑):
        - order-postgres → 호스트 5433 (DB: orders_db)
        - inventory-postgres → 호스트 5434 (DB: inventory_db)
        - payment-postgres → 호스트 5435 (DB: payment_db)
        - product-postgres → 호스트 5436 (DB: product_db)
    - 각 서비스 컨테이너: `order-service`, `inventory-service`, `payment-service`, `product-service` (compose가 각 서비스의
      `Dockerfile`로 빌드)

서비스 실행(루트에서):

```powershell
# 루트에서
docker compose build
docker compose up -d
```

서비스 포트(기본)

- product: 8080, inventory: 8081, payment: 8082, order: 8083

## 6) Docker화 및 CI

- 각 서비스에 `Dockerfile` 추가(Gradle wrapper + Java 17)
- 컨테이너 프로파일: `application-docker.yml` 사용 (`SPRING_PROFILES_ACTIVE=docker`)
- 루트 `docker-compose.yml`: Zookeeper + Kafka, Redis, 서비스별 Postgres, 그리고 각 서비스 빌드/런 정의
    - Compose는 각 서비스를 빌드(build: ./<service>)해 컨테이너를 생성합니다.
- CI: `.github/workflows/ci.yml` — `master` 푸시 시 각 서비스의 `./gradlew test` 실행 및 테스트 리포트 업로드
