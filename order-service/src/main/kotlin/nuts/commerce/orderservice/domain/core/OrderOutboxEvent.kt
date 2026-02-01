package nuts.commerce.orderservice.domain.core

import jakarta.persistence.*
import nuts.commerce.orderservice.domain.BaseEntity
import nuts.commerce.orderservice.domain.OutboxStatus
import java.time.Instant
import java.util.*
import kotlin.math.min

@Entity
@Table(name = "order_outbox_events")
class OrderOutboxEvent protected constructor() : BaseEntity() {

    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID()

    @Column(nullable = false, updatable = false)
    lateinit var aggregateId: UUID
        protected set

    @Column(nullable = false, updatable = false)
    lateinit var eventType: String
        protected set

    @Lob
    @Column(nullable = false, updatable = false)
    lateinit var payload: String
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OutboxStatus = OutboxStatus.PENDING
        protected set

    @Column(nullable = false)
    var retryCount: Int = 0
        protected set

    /**
     * - PENDING/FAILED: 다음 시도 시각(스케줄링)
     * - PUBLISHED/DEAD: null
     */
    @Column(nullable = true)
    var nextRetryAt: Instant? = null
        protected set

    @Column(nullable = true, length = 2000)
    var lastError: String? = null
        protected set

    companion object {
        fun create(
            aggregateId: UUID,
            eventType: String,
            payload: String,
            now: Instant = Instant.now(),
        ): OrderOutboxEvent =
            OrderOutboxEvent().apply {
                this.aggregateId = aggregateId
                this.eventType = eventType
                this.payload = payload
                this.status = OutboxStatus.PENDING
                this.retryCount = 0
                this.nextRetryAt = now
                this.lastError = null
            }
    }

    fun markPublished() {
        status = OutboxStatus.PUBLISHED
        lastError = null
        nextRetryAt = null
    }

    fun markFailed(
        error: String,
        now: Instant = Instant.now(),
        maxRetries: Int,
        baseDelaySeconds: Long = 1,
        maxDelaySeconds: Long = 60,
    ) {
        if (status == OutboxStatus.PUBLISHED || status == OutboxStatus.DEAD) return

        lastError = error
        retryCount += 1

        if (retryCount > maxRetries) {
            status = OutboxStatus.DEAD
            nextRetryAt = null
            return
        }

        status = OutboxStatus.FAILED

        val exp = (retryCount - 1).coerceAtLeast(0)
        val delaySeconds = min(maxDelaySeconds, baseDelaySeconds * (1L shl exp))
        nextRetryAt = now.plusSeconds(delaySeconds)
    }

    fun isReady(now: Instant = Instant.now()): Boolean =
        status != OutboxStatus.PUBLISHED &&
            status != OutboxStatus.DEAD &&
            nextRetryAt != null &&
            !nextRetryAt!!.isAfter(now)
}