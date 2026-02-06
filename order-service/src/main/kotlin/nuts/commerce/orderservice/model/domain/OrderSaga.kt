package nuts.commerce.orderservice.model.domain

import jakarta.persistence.*
import nuts.commerce.orderservice.model.BaseEntity
import nuts.commerce.orderservice.model.exception.OrderException
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

    companion object {
        fun create(orderId: UUID, idGenerator: () -> UUID = { UUID.randomUUID() }): OrderSaga {
            require(!orderId.toString().isBlank()) { "orderId is required" }

            return OrderSaga().apply {
                this.id = idGenerator()
                this.orderId = orderId
                this.status = SagaStatus.CREATED
            }
        }
    }

    fun markInventoryRequested() {
        if (status != SagaStatus.CREATED) {
            throw OrderException.InvalidTransition(idOrNull(), status.toOrderStatus(), Order.OrderStatus.PAYING)
        }
        status = SagaStatus.INVENTORY_REQUESTED
    }

    fun markInventoryReserved() {
        if (status != SagaStatus.CREATED && status != SagaStatus.INVENTORY_REQUESTED) {
            throw OrderException.InvalidTransition(idOrNull(), status.toOrderStatus(), Order.OrderStatus.PAYING)
        }
        status = SagaStatus.INVENTORY_RESERVED
    }

    fun markPaymentRequested() {
        if (status != SagaStatus.INVENTORY_RESERVED) {
            throw OrderException.InvalidTransition(idOrNull(), status.toOrderStatus(), Order.OrderStatus.PAYING)
        }
        status = SagaStatus.PAYMENT_REQUESTED
    }

    fun markPaymentCompleted() {
        if (status != SagaStatus.PAYMENT_REQUESTED) {
            throw OrderException.InvalidTransition(idOrNull(), status.toOrderStatus(), Order.OrderStatus.PAID)
        }
        status = SagaStatus.PAYMENT_COMPLETED
    }

    fun markInventoryReleased() {
        if (status == SagaStatus.COMPLETED || status == SagaStatus.FAILED) {
            throw OrderException.InvalidTransition(idOrNull(), status.toOrderStatus(), Order.OrderStatus.CANCELED)
        }
        status = SagaStatus.INVENTORY_RELEASED
    }

    fun markCompleted() {
        if (status != SagaStatus.PAYMENT_COMPLETED && status != SagaStatus.RESOLVE_TO_COMPLETE) {
            throw OrderException.InvalidTransition(idOrNull(), status.toOrderStatus(), Order.OrderStatus.PAID)
        }
        status = SagaStatus.COMPLETED
    }

    fun fail() {
        status = SagaStatus.FAILED
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
