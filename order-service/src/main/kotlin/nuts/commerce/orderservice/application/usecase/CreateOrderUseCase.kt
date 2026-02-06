package nuts.commerce.orderservice.application.usecase

import nuts.commerce.orderservice.application.port.repository.OrderOutboxRepository
import nuts.commerce.orderservice.application.port.repository.OrderRepository
import nuts.commerce.orderservice.application.port.repository.OrderSagaRepository
import nuts.commerce.orderservice.application.port.rest.ProductPriceSnapshot
import nuts.commerce.orderservice.application.port.rest.ProductRestClient
import nuts.commerce.orderservice.model.domain.Money
import nuts.commerce.orderservice.model.domain.Order
import nuts.commerce.orderservice.model.domain.OrderItem
import nuts.commerce.orderservice.model.domain.OrderSaga
import nuts.commerce.orderservice.model.exception.OrderException
import nuts.commerce.orderservice.model.infra.OutboxEventType
import nuts.commerce.orderservice.model.infra.OutboxRecord
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
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
    transactionManager: PlatformTransactionManager
) {
    // 기본 트랜잭션 템플릿 (주문+아웃박스 저장)
    private val txTemplate = TransactionTemplate(transactionManager)

    // 사가/상태전이용 별도 템플릿(새 트랜잭션, 짧은 타임아웃)
    private val sagaTxTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        timeout = 5
    }

    fun create(command: Command): Result {

        // 멱등성 체크(비트랜잭션) — 이미 생성된 주문이 있으면 바로 반환
        orderRepository.findByUserIdAndIdempotencyKey(command.userId, command.idempotencyKey)
            ?.let { return Result(it.id) }

        // 상품 ID 배치 조회
        val productIds = command.items.map { it.productId }.distinct()
        val snapshots: List<ProductPriceSnapshot> = try {
            productClient.getPriceSnapshots(productIds)
        } catch (_: Exception) {
            throw OrderException.InvalidCommand("failed to fetch product price snapshots")
        }
        val snapshotById = snapshots.associateBy { it.productId }

        // 주문 아이템 빌드 (트랜잭션 외부)
        val orderId = UUID.randomUUID()
        val orderItems: List<OrderItem> = command.items.map {
            val snap =
                snapshotById[it.productId] ?: throw OrderException.InvalidCommand("product not found: ${it.productId}")
            OrderItem.create(
                productId = it.productId,
                orderId = orderId,
                qty = it.qty,
                unitPrice = Money(snap.price, snap.currency)
            )
        }

        val reserveItems = orderItems.map { item ->
            val snap = snapshotById[item.productId]!!
            ReserveInventoryPayload.Item(item.productId, item.qty, snap.price, snap.currency)
        }
        val aggregatePayload = objectMapper.writeValueAsString(
            ReserveInventoryPayload(orderId = orderId, items = reserveItems)
        )

        val saved = txTemplate.execute {
            // 트랜잭션 내 멱등성 재확인
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
                // 이미 생성된 경우 기존 결과 반환
                return@execute orderRepository.findByUserIdAndIdempotencyKey(command.userId, command.idempotencyKey)!!
            }

            val outboxEvent = OutboxRecord.create(
                aggregateId = savedLocal.id,
                eventType = OutboxEventType.RESERVE_INVENTORY_REQUEST,
                payload = aggregatePayload
            )
            orderOutboxRepository.save(outboxEvent)

            return@execute savedLocal
        }!!

        // 트랜잭션 B: 사가 생성 및 주문 상태 전이(짧게 유지, REQUIRES_NEW)
        sagaTxTemplate.execute<Unit> {
            val saga = OrderSaga.create(saved.id)
            saga.markInventoryRequested()
            orderSagaRepository.save(saga)

            val order = orderRepository.findById(saved.id)
                ?: throw OrderException.InvalidCommand("order not found: ${saved.id}")
            order.markPaying()
            orderRepository.save(order)
        }

        return Result(saved.id)
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