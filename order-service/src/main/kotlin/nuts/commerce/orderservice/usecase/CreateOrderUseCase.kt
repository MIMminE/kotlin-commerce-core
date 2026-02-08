package nuts.commerce.orderservice.usecase

import nuts.commerce.orderservice.event.EventType
import nuts.commerce.orderservice.port.repository.OrderOutboxRepository
import nuts.commerce.orderservice.port.repository.OrderRepository
import nuts.commerce.orderservice.port.repository.OrderSagaRepository
import nuts.commerce.orderservice.port.rest.ProductPriceSnapshot
import nuts.commerce.orderservice.port.rest.ProductRestClient
import nuts.commerce.orderservice.model.Money
import nuts.commerce.orderservice.model.Order
import nuts.commerce.orderservice.model.OrderItem
import nuts.commerce.orderservice.model.OrderSaga
import nuts.commerce.orderservice.exception.OrderException
import nuts.commerce.orderservice.model.OutboxRecord
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class CreateOrderUseCase(
    private val orderRepository: OrderRepository,
    private val orderOutboxRepository: OrderOutboxRepository,
    private val orderSagaRepository: OrderSagaRepository,
    private val productClient: ProductRestClient,
    private val objectMapper: ObjectMapper,
    private val txTemplate: TransactionTemplate,
) {

    fun create(command: Command): Result {

        checkExistingOrder(command.userId, command.idempotencyKey)?.let { return Result(it.id) }

        // 상품 스냅샷 조회
        val productIds = command.items.map { it.productId }.distinct()
        val snapshotById = fetchSnapshots(productIds)

        // 주문 아이템 생성 (트랜잭션 외)
        val orderId = UUID.randomUUID()
        val orderItems = buildOrderItems(command, orderId, snapshotById)

        // ReserveInventory 페이로드 생성
        val aggregatePayload = buildReservePayload(orderId, orderItems, snapshotById)

        // 트랜잭션 내 주문 저장 및 outbox 발행
        val saved = saveOrderAndPublishOutbox(orderItems, command, orderId, aggregatePayload)

        // 트랜잭션 내 후처리: saga 생성, 주문 상태 변경
        postCreateTransactions(saved)

        return Result(saved.id)
    }

    private fun checkExistingOrder(userId: String, idempotencyKey: UUID) =
        orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)

    private fun fetchSnapshots(productIds: List<String>): Map<String, ProductPriceSnapshot> {
        val snapshots: List<ProductPriceSnapshot> = try {
            productClient.getPriceSnapshots(productIds)
        } catch (_: Exception) {
            throw OrderException.InvalidCommand("failed to fetch product price snapshots")
        }
        return snapshots.associateBy { it.productId }
    }

    private fun buildOrderItems(
        command: Command,
        orderId: UUID,
        snapshotById: Map<String, ProductPriceSnapshot>
    ): List<OrderItem> = command.items.map {
        val snap = snapshotById[it.productId]
            ?: throw OrderException.InvalidCommand("product not found: ${it.productId}")
        OrderItem.create(
            productId = it.productId,
            orderId = orderId,
            qty = it.qty,
            unitPrice = Money(snap.price, snap.currency)
        )
    }

    private fun buildReservePayload(
        orderId: UUID,
        orderItems: List<OrderItem>,
        snapshotById: Map<String, ProductPriceSnapshot>
    ): String {
        val reserveItems = orderItems.map { item ->
            val snap = snapshotById[item.productId]!!
            ReserveInventoryPayload.Item(item.productId, item.qty, snap.price, snap.currency)
        }
        return objectMapper.writeValueAsString(
            ReserveInventoryPayload(orderId = orderId, items = reserveItems)
        )
    }

    private fun saveOrderAndPublishOutbox(
        orderItems: List<OrderItem>,
        command: Command,
        orderId: UUID,
        aggregatePayload: String
    ): Order {
        return txTemplate.execute {
            orderRepository.findByUserIdAndIdempotencyKey(command.userId, command.idempotencyKey)
                ?.let { return@execute it }

            val order = Order.create(
                userId = command.userId,
                items = orderItems,
                total = Money(command.totalAmount, command.currency),
                idGenerator = { orderId },
                idempotencyKey = command.idempotencyKey
            )

            val savedLocal = try {
                orderRepository.save(order)
            } catch (_: DataIntegrityViolationException) {
                return@execute orderRepository.findByUserIdAndIdempotencyKey(command.userId, command.idempotencyKey)!!
            }

            val outboxEvent = OutboxRecord.create(
                aggregateId = savedLocal.id,
                eventType = EventType.RESERVE_INVENTORY_REQUEST,
                payload = aggregatePayload
            )
            orderOutboxRepository.save(outboxEvent)

            return@execute savedLocal
        }
    }

    private fun postCreateTransactions(saved: Order) {
        txTemplate.execute<Unit> {
            val saga = OrderSaga.create(saved.id)
            saga.markInventoryRequested()
            orderSagaRepository.save(saga)

            val order = orderRepository.findById(saved.id)
                ?: throw OrderException.InvalidCommand("order not found: ${saved.id}")
            order.markPaying()
            orderRepository.save(order)
        }
    }
}


data class Command(
    val idempotencyKey: UUID,
    val userId: String,
    val items: List<Item>,
    val totalAmount: Long,
    val currency: String,
)

data class Item(
    val productId: String,
    val qty: Int,
    val unitPriceAmount: Long,
    val unitPriceCurrency: String,
)

data class ReserveInventoryPayload(val orderId: UUID, val items: List<Item>) {
    data class Item(val productId: String, val qty: Int, val price: Long, val currency: String)
}

data class Result(val orderId: UUID)