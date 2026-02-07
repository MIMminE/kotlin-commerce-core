package nuts.commerce.orderservice.model.domain

import jakarta.persistence.*
import nuts.commerce.orderservice.model.BaseEntity
import nuts.commerce.orderservice.model.exception.OrderException
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "order_sagas")
class OrderSaga protected constructor() : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    lateinit var id: UUID
        protected set

    @Column(name = "order_id", nullable = false, updatable = false)
    lateinit var orderId: UUID
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: SagaStatus = SagaStatus.CREATED
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0L
        protected set

    @Column(name = "order_event_received_at")
    var orderEventReceivedAt: Instant? = null
        protected set

    @Column(name = "inventory_requested_at")
    var inventoryRequestedAt: Instant? = null
        protected set

    @Column(name = "inventory_reserved_at")
    var inventoryReservedAt: Instant? = null
        protected set

    @Column(name = "payment_requested_at")
    var paymentRequestedAt: Instant? = null
        protected set

    @Column(name = "payment_completed_at")
    var paymentCompletedAt: Instant? = null
        protected set

    @Column(name = "inventory_released_at")
    var inventoryReleasedAt: Instant? = null
        protected set

    @Column(name = "completed_at")
    var completedAt: Instant? = null
        protected set

    @Column(name = "failed_at")
    var failedAt: Instant? = null
        protected set

    companion object {
        fun create(
            orderId: UUID,
            status: SagaStatus = SagaStatus.CREATED,
            idGenerator: () -> UUID = { UUID.randomUUID() }
        ): OrderSaga {

            return OrderSaga().apply {
                this.id = idGenerator()
                this.orderId = orderId
                this.status = status
            }
        }
    }

    fun markOrderEventReceived(at: Instant = Instant.now()) {
        this.orderEventReceivedAt = at
    }

    fun markInventoryRequested(at: Instant = Instant.now()) {
        if (status != SagaStatus.CREATED) {
            throw OrderException.InvalidTransition(idOrNull(), status.toOrderStatus(), Order.OrderStatus.PAYING)
        }
        status = SagaStatus.INVENTORY_REQUESTED
        this.inventoryRequestedAt = at
    }

    fun markInventoryReserved(at: Instant = Instant.now()) {
        if (status != SagaStatus.CREATED && status != SagaStatus.INVENTORY_REQUESTED) {
            throw OrderException.InvalidTransition(idOrNull(), status.toOrderStatus(), Order.OrderStatus.PAYING)
        }
        status = SagaStatus.INVENTORY_RESERVED
        this.inventoryReservedAt = at
    }

    fun markPaymentRequested(at: Instant = Instant.now()) {
        if (status != SagaStatus.INVENTORY_RESERVED) {
            throw OrderException.InvalidTransition(idOrNull(), status.toOrderStatus(), Order.OrderStatus.PAYING)
        }
        status = SagaStatus.PAYMENT_REQUESTED
        this.paymentRequestedAt = at
    }

    fun markPaymentCompleted(at: Instant = Instant.now()) {
        if (status != SagaStatus.PAYMENT_REQUESTED) {
            throw OrderException.InvalidTransition(idOrNull(), status.toOrderStatus(), Order.OrderStatus.PAID)
        }
        status = SagaStatus.PAYMENT_COMPLETED
        this.paymentCompletedAt = at
    }

    fun markInventoryReleased(at: Instant = Instant.now()) {
        if (status == SagaStatus.COMPLETED || status == SagaStatus.FAILED) {
            throw OrderException.InvalidTransition(idOrNull(), status.toOrderStatus(), Order.OrderStatus.CANCELED)
        }
        status = SagaStatus.INVENTORY_RELEASED
        this.inventoryReleasedAt = at
    }

    fun markCompleted(at: Instant = Instant.now()) {
        if (status != SagaStatus.PAYMENT_COMPLETED && status != SagaStatus.RESOLVE_TO_COMPLETE) {
            throw OrderException.InvalidTransition(idOrNull(), status.toOrderStatus(), Order.OrderStatus.PAID)
        }
        status = SagaStatus.COMPLETED
        this.completedAt = at
    }

    fun fail(at: Instant = Instant.now()) {
        status = SagaStatus.FAILED
        this.failedAt = at
    }

    private fun idOrNull(): UUID? = if (this::id.isInitialized) id else null

    enum class SagaStatus {
        CREATED,
        INVENTORY_REQUESTED,
        INVENTORY_RESERVED,
        PAYMENT_REQUESTED,
        PAYMENT_COMPLETED,
        INVENTORY_RELEASED,
        COMPLETED,
        FAILED,
        RESOLVE_TO_COMPLETE;

        fun toOrderStatus(): Order.OrderStatus {
            return when (this) {
                CREATED -> Order.OrderStatus.CREATED
                INVENTORY_REQUESTED, INVENTORY_RESERVED, PAYMENT_REQUESTED -> Order.OrderStatus.PAYING
                PAYMENT_COMPLETED -> Order.OrderStatus.PAID
                INVENTORY_RELEASED -> Order.OrderStatus.PAYMENT_FAILED
                COMPLETED -> Order.OrderStatus.PAID
                FAILED -> Order.OrderStatus.CANCELED
                RESOLVE_TO_COMPLETE -> Order.OrderStatus.PAID
            }
        }
    }
}