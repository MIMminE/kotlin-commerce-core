package nuts.commerce.orderservice.model

import jakarta.persistence.*
import nuts.commerce.orderservice.event.outbound.OutboundEventType
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "order_outbox_records",
    uniqueConstraints = [UniqueConstraint(columnNames = ["orderId", "idempotency_key"])]

)
class OutboxRecord protected constructor(
    @Id
    val outboxId: UUID,

    @Column(nullable = false, updatable = false)
    val orderId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false)
    val eventType: OutboundEventType,

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    val idempotencyKey: UUID,

    @Lob
    @Column(nullable = false)
    var payload: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OutboxStatus,

    @Column(name = "locked_by")
    var lockedBy: String?,

    @Column(name = "locked_until")
    var lockedUntil: Instant?,

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int,

    @Column(name = "next_attempt_at", nullable = false)
    var nextAttemptAt: Instant

) : BaseEntity() {

    companion object {
        fun create(
            outboxId: UUID = UUID.randomUUID(),
            orderId: UUID,
            eventType: OutboundEventType,
            idempotencyKey: UUID,
            payload: String,
            status: OutboxStatus = OutboxStatus.PENDING,
            lockedBy: String? = null,
            lockedUntil: Instant? = null,
            attemptCount: Int = 0,
            nextAttemptAt: Instant = Instant.now(),
        ): OutboxRecord {
            return OutboxRecord(
                outboxId = outboxId,
                orderId = orderId,
                idempotencyKey = idempotencyKey,
                eventType = eventType,
                payload = payload,
                status = status,
                lockedBy = lockedBy,
                lockedUntil = lockedUntil,
                attemptCount = attemptCount,
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
        updatedAt = now
    }

    fun scheduleRetry(now: Instant, nextAttemptAt: Instant, error: String? = null) {
        require(status == OutboxStatus.PROCESSING) { "invalid transition $status -> RETRY_SCHEDULED" }
        status = OutboxStatus.RETRY_SCHEDULED
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

data class OutboxInfo(
    val outboxId: UUID,
    val orderId: UUID,
    val eventType: OutboundEventType,
    val payload: String
)