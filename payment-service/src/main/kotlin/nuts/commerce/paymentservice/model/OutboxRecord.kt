package nuts.commerce.paymentservice.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "payment_outbox_records")
class OutboxRecord protected constructor(
    @Id
    val outboxId: UUID,

    @Column(nullable = false, updatable = false)
    val aggregateId: UUID,

    @Column(nullable = false)
    val eventType: String,

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
            eventType: String,
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
        updatedAt = now
    }

    fun scheduleRetry(now: Instant, nextAttemptAt: Instant) {
        require(status == OutboxStatus.PROCESSING) { "invalid transition $status -> RETRY_SCHEDULED" }
        attempts += 1
        status = OutboxStatus.RETRY_SCHEDULED
        this.nextAttemptAt = nextAttemptAt
        updatedAt = now
    }

    fun markFailed(now: Instant) {
        require(status == OutboxStatus.PROCESSING) { "invalid transition $status -> FAILED" }
        status = OutboxStatus.FAILED
        updatedAt = now
    }
}

enum class OutboxStatus { PENDING, PROCESSING, PUBLISHED, FAILED, RETRY_SCHEDULED }

