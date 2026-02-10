package nuts.commerce.paymentservice.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "payment_outbox_records",
    uniqueConstraints = [UniqueConstraint(columnNames = ["payment_id", "idempotency_key"])]
)
class OutboxRecord protected constructor(
    @Id
    val outboxId: UUID,

    @Column(nullable = false, updatable = false)
    val orderId: UUID,

    @Column(nullable = false, updatable = false)
    val paymentId: UUID,

    @Column(nullable = false, updatable = false)
    val idempotencyKey: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val eventType: EventType,

    @Lob
    @Column(nullable = false)
    var payload: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OutboxStatus,

    var lockedBy: String?,

    var lockedUntil: Instant?,

    @Column(nullable = false)
    var attemptCount: Int,

    @Column(nullable = false)
    var nextAttemptAt: Instant,

    ) : BaseEntity() {

    companion object {

        fun create(
            outboxId: UUID = UUID.randomUUID(),
            orderId: UUID,
            paymentId: UUID,
            idempotencyKey: UUID,
            eventType: EventType,
            payload: String,
            status: OutboxStatus = OutboxStatus.PENDING,
            lockedBy: String? = null,
            lockedUntil: Instant? = null,
            attemptCount: Int = 0,
            nextAttemptAt: Instant = Instant.now()
        ): OutboxRecord =
            OutboxRecord(
                outboxId = outboxId,
                orderId = orderId,
                paymentId = paymentId,
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

enum class OutboxStatus { PENDING, PROCESSING, PUBLISHED, FAILED, RETRY_SCHEDULED }

