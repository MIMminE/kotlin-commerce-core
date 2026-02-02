package nuts.commerce.orderservice.model.integration

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import nuts.commerce.orderservice.model.BaseEntity
import java.time.Instant
import java.util.UUID
import kotlin.math.min

@Entity
@Table(name = "order_outbox_events")
class OrderOutboxRecord protected constructor() : BaseEntity() {

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
        ): OrderOutboxRecord =
            OrderOutboxRecord().apply {
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

    enum class OutboxStatus {
        PENDING,    // 아직 발행 시도 전(또는 즉시 처리 대기)
        PROCESSING, // 발행 시도 중
        PUBLISHED,  // 발행 완료
        FAILED,     // 마지막 시도가 실패(재시도 예정일 수 있음)
        DEAD        // 최대 재시도 초과 등으로 더 이상 처리하지 않음
    }
}

