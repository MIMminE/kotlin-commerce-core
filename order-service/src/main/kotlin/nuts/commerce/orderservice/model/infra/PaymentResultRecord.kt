package nuts.commerce.orderservice.model.infra

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import nuts.commerce.orderservice.model.BaseEntity
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "payment_result_events")
class PaymentResultRecord protected constructor() : BaseEntity() {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    lateinit var eventId: UUID
        protected set

    @Column(name = "order_id", nullable = false, updatable = false)
    lateinit var orderId: UUID
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64, updatable = false)
    lateinit var eventType: EventType
        protected set

    @Column(name = "payload", nullable = false, columnDefinition = "text", updatable = false)
    lateinit var payload: String
        protected set

    @Column(name = "received_at", nullable = false, updatable = false)
    var receivedAt: Instant = Instant.EPOCH
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: Status = Status.RECEIVED
        protected set

    @Column(name = "processed_at")
    var processedAt: Instant? = null
        protected set

    @Column(name = "last_error", length = 500)
    var lastError: String? = null
        protected set

    fun markProcessed(at: Instant = Instant.now()) {
        status = Status.PROCESSED
        processedAt = at
        lastError = null
    }

    fun markFailed(error: String, at: Instant = Instant.now()) {
        status = Status.FAILED
        processedAt = at
        lastError = error.take(500)
    }

    fun isProcessed(): Boolean = status == Status.PROCESSED

    enum class EventType { PAYMENT_SUCCESS, PAYMENT_FAILURE }
    enum class Status { RECEIVED, PROCESSED, FAILED }

    companion object {
        fun receive(
            eventId: UUID,
            orderId: UUID,
            eventType: EventType,
            payload: String,
            receivedAt: Instant = Instant.now()
        ): PaymentResultRecord {

            return PaymentResultRecord().apply {
                this.eventId = eventId
                this.orderId = orderId
                this.eventType = eventType
                this.payload = payload
                this.receivedAt = receivedAt
                this.status = Status.RECEIVED
            }
        }
    }
}