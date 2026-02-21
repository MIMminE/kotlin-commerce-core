package nuts.commerce.orderservice.usecase

import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.OutboundReservationItem
import nuts.commerce.orderservice.event.outbound.ReservationCreatePayload
import nuts.commerce.orderservice.port.repository.OutboxRepository
import nuts.commerce.orderservice.port.repository.OrderRepository
import nuts.commerce.orderservice.port.repository.SageRepository
import nuts.commerce.orderservice.port.rest.ProductPriceSnapshot
import nuts.commerce.orderservice.port.rest.ProductRestClient
import nuts.commerce.orderservice.model.Money
import nuts.commerce.orderservice.model.Order
import nuts.commerce.orderservice.model.OrderItem
import nuts.commerce.orderservice.model.OrderSaga
import nuts.commerce.orderservice.exception.OrderException
import nuts.commerce.orderservice.model.OutboxRecord
import nuts.commerce.orderservice.port.rest.ProductPriceResponse
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

@Service
class OrderCreateUseCase(
    private val productClient: ProductRestClient,
    private val orderRepository: OrderRepository,
    private val sageRepository: SageRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper,
    private val txTemplate: TransactionTemplate,
) {

    fun create(command: OrderCreateCommand): OrderCreateResult {

        // 상품 스냅샷 조회
        val productIds = command.items.map { it.productId }
        val productPriceResponse = productClient.getPriceSnapshots(productIds)
        if (!productPriceResponse.isSuccess) {
            throw OrderException.InvalidCommand("failed to fetch product price snapshots")
        }

        val orderId =
            txTemplate.execute { saveOrderWithIdempotencyCheck(command, productPriceResponse) }.let {
                if (!it.isNewlyCreated) {
                    return OrderCreateResult(it.orderId)
                }
                return@let it.orderId
            }

        val payload = ReservationCreatePayload(
            reservationItems = command.items.map {
                val snap = productPriceResponse.productPriceSnapshot.find { ps -> ps.productId == it.productId }
                    ?: throw OrderException.InvalidCommand("product not found: ${it.productId}")

                OutboundReservationItem(
                    productId = it.productId,
                    qty = it.qty,
                    price = snap.price,
                    currency = snap.currency
                )
            }
        )

        val outbox = OutboxRecord.create(
            orderId = orderId,
            idempotencyKey = command.idempotencyKey,
            eventType = OutboundEventType.RESERVATION_CREATE_REQUEST,
            payload = objectMapper.writeValueAsString(payload)
        )

        val totalPrice = payload.reservationItems.sumOf { it.price * it.qty }
        val sage = OrderSaga.create(
            orderId = orderId,
            totalPrice = totalPrice,
            currency = command.currency,
            reservationRequestedAt = Instant.now()
        )

        txTemplate.execute {
            outboxRepository.save(outbox)
            sageRepository.save(sage)
        }
        return OrderCreateResult(orderId)
    }

    private fun saveOrderWithIdempotencyCheck(
        command: OrderCreateCommand,
        productPriceResponse: ProductPriceResponse
    ): OrderSaveResult {
        val snapshotProduct = productPriceResponse.productPriceSnapshot.associateBy { it.productId }
        val orderItems = buildOrderItems(command, snapshotProduct)

        val order = Order.create(
            userId = command.userId,
            items = orderItems,
            idempotencyKey = command.idempotencyKey
        )

        try {
            val savedOrder = orderRepository.save(order)
            return OrderSaveResult(savedOrder.orderId, isNewlyCreated = true)
        } catch (e: DataIntegrityViolationException) {
            val existing = orderRepository.findByUserIdAndIdempotencyKey(command.userId, command.idempotencyKey)
            if (existing != null) {
                return OrderSaveResult(existing.orderId, isNewlyCreated = false)
            } else {
                throw e
            }
        }
    }

    private fun buildOrderItems(
        orderCreateCommand: OrderCreateCommand,
        snapshotById: Map<UUID, ProductPriceSnapshot>
    ): List<OrderItem> = orderCreateCommand.items.map {
        val snap = snapshotById[it.productId]
            ?: throw OrderException.InvalidCommand("product not found: ${it.productId}")
        OrderItem.create(
            productId = it.productId,
            qty = it.qty,
            unitPrice = Money(snap.price, snap.currency)
        )
    }

    data class OrderSaveResult(val orderId: UUID, val isNewlyCreated: Boolean)
}


data class OrderCreateCommand(
    val idempotencyKey: UUID,
    val userId: String,
    val items: List<Item>,
    val totalAmount: Long,
    val currency: String,
)

data class Item(
    val productId: UUID,
    val qty: Long,
    val unitPriceAmount: Long,
    val unitPriceCurrency: String,
)

data class ReserveInventoryPayload(val orderId: UUID, val items: List<Item>) {
    data class Item(val productId: String, val qty: Long, val price: Long, val currency: String)
}

data class OrderCreateResult(val orderId: UUID)