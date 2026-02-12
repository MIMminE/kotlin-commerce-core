package nuts.commerce.inventoryservice.model

import jakarta.persistence.*
import nuts.commerce.inventoryservice.model.EventType
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "inventory_outbox_records",
    uniqueConstraints = [UniqueConstraint(columnNames = ["reservation_id", "idempotency_key"])]
)
class OutboxRecord protected constructor(
    @Id
    val outboxId: UUID,

    @Column(name = "order_id", nullable = false, updatable = false)
    val orderId: UUID,

    @Column(name = "reservation_id", nullable = false, updatable = false)
    val reservationId: UUID,

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    val idempotencyKey: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false)
    val eventType: EventType,

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
    var nextAttemptAt: Instant,

    ) : BaseEntity() {

    companion object {

        fun create(
            outboxId: UUID = UUID.randomUUID(),
            orderId: UUID,
            reservationId: UUID,
            idempotencyKey: UUID,
            eventType: EventType,
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
                reservationId = reservationId,
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
}

enum class OutboxStatus { PENDING, PROCESSING, PUBLISHED, FAILED, RETRY_SCHEDULED }
data class OutboxInfo(
    val outboxId: UUID,
    val orderId: UUID,
    val reservationId: UUID,
    val eventType: EventType,
    val payload: String
)