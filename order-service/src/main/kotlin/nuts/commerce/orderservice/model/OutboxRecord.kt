package nuts.commerce.orderservice.model

import jakarta.persistence.*
import nuts.commerce.orderservice.event.EventType
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "order_outbox_events")
class OutboxRecord protected constructor(
    @Id
    val outboxId: UUID,

    @Column(nullable = false, updatable = false)
    val aggregateId: UUID,

    @Column(name = "event_type", nullable = false, updatable = false)
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
            eventType: EventType,
            payload: String,
            attempts: Int = 0,
            status: OutboxStatus = OutboxStatus.PENDING,
            nextAttemptAt: Instant? = null
        ): OutboxRecord {
            return OutboxRecord(
                outboxId = id,
                aggregateId = aggregateId,
                eventType = eventType.name,
                payload = payload,
                status = status,
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