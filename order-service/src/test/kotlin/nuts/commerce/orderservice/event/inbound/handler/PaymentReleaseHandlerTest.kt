package nuts.commerce.orderservice.event.inbound.handler

import nuts.commerce.orderservice.event.inbound.InboundEventType
import nuts.commerce.orderservice.event.inbound.OrderInboundEvent
import nuts.commerce.orderservice.event.inbound.PaymentReleaseSuccessPayload
import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.model.Order
import nuts.commerce.orderservice.model.OrderSaga
import nuts.commerce.orderservice.model.OrderStatus
import nuts.commerce.orderservice.testutil.InMemoryOrderRepository
import nuts.commerce.orderservice.testutil.InMemoryOutboxRepository
import nuts.commerce.orderservice.testutil.InMemorySageRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("NonAsciiCharacters")
class PaymentReleaseHandlerTest {

    private lateinit var orderRepository: InMemoryOrderRepository
    private lateinit var sageRepository: InMemorySageRepository
    private lateinit var outboxRepository: InMemoryOutboxRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var handler: PaymentReleaseHandler

    @BeforeEach
    fun setUp() {
        orderRepository = InMemoryOrderRepository()
        sageRepository = InMemorySageRepository()
        outboxRepository = InMemoryOutboxRepository()
        objectMapper = ObjectMapper()
        handler = PaymentReleaseHandler(
            orderRepository,
            sageRepository,
            outboxRepository,
            objectMapper
        )
    }

    @Test
    fun `결제 해제 성공 - 주문 상태가 PAYMENT_FAILED로 변경되고 예약 해제 요청 outbox가 생성된다`() {
        // given
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        val order = Order.create(
            orderId = orderId,
            idempotencyKey = UUID.randomUUID(),
            userId = "user123",
            status = OrderStatus.PAYING
        )
        orderRepository.save(order)

        val saga = OrderSaga.create(
            orderId = orderId,
            totalPrice = 10000,
            currency = "KRW",
            reservationRequestedAt = Instant.now()
        )
        saga.reservationId = reservationId
        sageRepository.save(saga)

        val event = OrderInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.PAYMENT_RELEASE,
            payload = PaymentReleaseSuccessPayload(
                reason = "payment timeout"
            )
        )

        // when
        handler.handle(event)

        // then
        val updatedOrder = orderRepository.findById(orderId)
        assertNotNull(updatedOrder)
        assertEquals(OrderStatus.PAYMENT_FAILED, updatedOrder.status)

        val updatedSaga = sageRepository.findByOrderId(orderId)
        assertNotNull(updatedSaga)
        assertNotNull(updatedSaga.reservationReleasedAt)

        val outboxRecords = outboxRepository.findAll()
        assertEquals(1, outboxRecords.size)
        val outboxRecord = outboxRecords.first()
        assertEquals(orderId, outboxRecord.orderId)
        assertEquals(eventId, outboxRecord.idempotencyKey)
        assertEquals(OutboundEventType.RESERVATION_RELEASE_REQUEST, outboxRecord.eventType)
    }
}

