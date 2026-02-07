package nuts.commerce.inventoryservice.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID
import tools.jackson.databind.ObjectMapper

@Entity
@Table(name = "inventory_outbox_records")
class OutboxRecord protected constructor(
    @Id
    val outboxId: UUID,

    @Column(name = "reservation_id", nullable = false, updatable = false)
    val reservationId: UUID,

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

    ) : BaseEntity() {

    companion object {

        fun createWithPayload(
            reservationId: UUID,
            eventType: OutboxEventType,
            payloadObj: Any,
            objectMapper: ObjectMapper,
            outboxId: UUID = UUID.randomUUID(),
            attempts: Int = 0,
            status: OutboxStatus = OutboxStatus.PENDING,
            nextAttemptAt: Instant? = null
        ): OutboxRecord {
            val payloadStr = objectMapper.writeValueAsString(payloadObj)
            return OutboxRecord(
                outboxId = outboxId,
                reservationId = reservationId,
                eventType = eventType,
                payload = payloadStr,
                attempts = attempts,
                status = status,
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

enum class OutboxEventType {
    INVENTORY_UPDATED,
    RESERVATION_CREATED,
    RESERVATION_COMMITTED,
    RESERVATION_RELEASED
}
