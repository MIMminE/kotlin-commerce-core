package nuts.commerce.orderservice.application.usecase

import nuts.commerce.orderservice.application.port.repository.InMemoryOrderRepository
import nuts.commerce.orderservice.application.port.repository.InMemoryPaymentResultRecordRepository
import nuts.commerce.orderservice.model.domain.Money
import nuts.commerce.orderservice.model.domain.Order
import nuts.commerce.orderservice.model.domain.OrderItem
import nuts.commerce.orderservice.model.integration.PaymentResultRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class OnPaymentApprovedUseCaseTest {

    private val orderRepository = InMemoryOrderRepository()
    private val paymentRecordRepo = InMemoryPaymentResultRecordRepository()
    private val useCase = OnPaymentApprovedUseCase(orderRepository, paymentRecordRepo)

    @BeforeEach
    fun setup() {
        orderRepository.clear()
        paymentRecordRepo.clear()
    }

    @Test
    fun `신규 결제 승인 이벤트이면 주문이 PAID 처리되고 record는 PROCESSED가 된다`() {
        val orderId = UUID.randomUUID()
        val items = listOf(OrderItem.create("p-1", orderId, 1, Money(1000L, "KRW")))
        val order = Order.create(
            userId = "u-1",
            idempotencyKey = UUID.randomUUID(),
            items = items,
            total = Money(1000L, "KRW"), status = Order.OrderStatus.PAYING, idGenerator = { orderId })
        orderRepository.save(order)

        val event = OnPaymentApprovedUseCase.PaymentApprovedEvent(
            eventId = UUID.randomUUID(),
            orderId = orderId,
            paymentId = UUID.randomUUID(),
            payload = "{}"
        )

        useCase.handle(event)

        val savedOrder = orderRepository.findById(orderId) ?: fail("order not found")
        assertEquals(Order.OrderStatus.PAID, savedOrder.status)

        val records = paymentRecordRepo.listByOrder(orderId)
        assertEquals(1, records.size)
        val rec = records.first()
        assertEquals(PaymentResultRecord.Status.PROCESSED, rec.status)
        assertNotNull(rec.processedAt)
    }

    @Test
    fun `중복 event면 아무 동작도 하지 않는다`() {
        val orderId = UUID.randomUUID()
        val items = listOf(OrderItem.create("p-1", orderId, 1, Money(1000L, "KRW")))
        val order = Order.create(
            userId = "u-1",
            idempotencyKey = UUID.randomUUID(),
            items = items,
            total = Money(1000L, "KRW"),
            status = Order.OrderStatus.PAYING,
            idGenerator = { orderId })
        orderRepository.save(order)

        val eventId = UUID.randomUUID()
        val event = OnPaymentApprovedUseCase.PaymentApprovedEvent(eventId, orderId, UUID.randomUUID(), "{}")

        useCase.handle(event)
        useCase.handle(event)

        val records = paymentRecordRepo.listByOrder(orderId)
        assertEquals(1, records.size)
    }

    @Test
    fun `order가 없으면 record는 FAILED로 저장된다`() {
        val orderId = UUID.randomUUID()
        val event = OnPaymentApprovedUseCase.PaymentApprovedEvent(UUID.randomUUID(), orderId, UUID.randomUUID(), "{}")

        useCase.handle(event)

        val records = paymentRecordRepo.listByOrder(orderId)
        assertEquals(1, records.size)
        val rec = records.first()
        assertEquals(PaymentResultRecord.Status.FAILED, rec.status)
        assertNotNull(rec.lastError)
    }

    @Test
    fun `이미 PAID인 주문이면 record는 FAILED로 저장된다`() {
        val orderId = UUID.randomUUID()
        val items = listOf(OrderItem.create("p-1", orderId, 1, Money(1000L, "KRW")))
        val order = Order.create(
            userId = "u-1",
            idempotencyKey = UUID.randomUUID(),
            items = items,
            total = Money(1000L, "KRW"),
            status = Order.OrderStatus.PAYING,
            idGenerator = { orderId })

        order.applyPaymentApproved()
        orderRepository.save(order)

        val event = OnPaymentApprovedUseCase.PaymentApprovedEvent(UUID.randomUUID(), orderId, UUID.randomUUID(), "{}")

        useCase.handle(event)

        val rec = paymentRecordRepo.listByOrder(orderId).first()
        assertEquals(PaymentResultRecord.Status.FAILED, rec.status)
        assertTrue(rec.lastError!!.contains("already PAID") || rec.lastError!!.isNotBlank())
    }
}