package nuts.commerce.orderservice.model.infra

import jakarta.persistence.*
import nuts.commerce.orderservice.model.BaseEntity
import java.time.Instant
import java.util.UUID

enum class OutboxEventType {
    // 주문-도메인 아웃박스 이벤트 타입
    RESERVE_INVENTORY_REQUEST,   // 재고 예약 요청
    PAYMENT_REQUEST,             // 결제 요청
    RESERVE_INVENTORY_CONFIRM,   // 재고 예약 확정
    RESERVE_INVENTORY_RELEASE,   // 재고 예약 반환
    PAYMENT_COMPLETED,           // 결제 완료

    // 기존 사용성을 위해 유지(호환성) - 필요 없으면 제거 가능
    ORDER_CREATED
}

@Entity
@Table(name = "order_outbox_events")
class OutboxRecord protected constructor(
    @Id
    val outboxId: UUID,

    @Column(nullable = false, updatable = false)
    val aggregateId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false)
    val eventType: OutboxEventType,

    @Lob
    @Column(nullable = false)
    var payload: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OutboxStatus,

    @Column(nullable = false)
    var attempts: Int,

    @Column(nullable = true)
    var nextAttemptAt: Instant?,

    @Version
    var version: Long? = null
) : BaseEntity() {

    companion object {
        fun create(
            id: UUID = UUID.randomUUID(),
            aggregateId: UUID,
            eventType: OutboxEventType,
            payload: String,
            attempts: Int = 0,
            nextAttemptAt: Instant? = null
        ): OutboxRecord {
            return OutboxRecord(
                outboxId = id,
                aggregateId = aggregateId,
                eventType = eventType,
                payload = payload,
                status = OutboxStatus.PENDING,
                attempts = attempts,
                nextAttemptAt = nextAttemptAt
            )
        }
    }

    fun startProcessing(now: Instant) {
        require(status == OutboxStatus.PENDING || status == OutboxStatus.RETRY_SCHEDULED) {
            "invalid transition $status -> PROCESSING"
        }
        status = OutboxStatus.PROCESSING
        updatedAt = now
    }

    fun markPublished(now: Instant) {
        require(status == OutboxStatus.PROCESSING) { "invalid transition $status -> PUBLISHED" }
        status = OutboxStatus.PUBLISHED
        nextAttemptAt = null
        updatedAt = now
    }

    fun scheduleRetry(now: Instant, nextAttemptAt: Instant, error: String? = null) {
        require(status == OutboxStatus.PROCESSING) { "invalid transition $status -> RETRY_SCHEDULED" }
        attempts += 1
        status = OutboxStatus.RETRY_SCHEDULED
        this.nextAttemptAt = nextAttemptAt
        updatedAt = now
    }

    fun markFailed(now: Instant, error: String? = null) {
        require(status == OutboxStatus.PROCESSING) { "invalid transition $status -> FAILED" }
        status = OutboxStatus.FAILED
        updatedAt = now
    }

}

enum class OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED,
    RETRY_SCHEDULED
}