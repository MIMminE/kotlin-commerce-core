package nuts.commerce.orderservice.event.inbound.handler

import nuts.commerce.orderservice.event.inbound.InboundEventType
import nuts.commerce.orderservice.event.inbound.OrderInboundEvent
import nuts.commerce.orderservice.event.inbound.ReservationConfirmSuccessPayload
import nuts.commerce.orderservice.event.inbound.InboundReservationItem
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
class ReservationConfirmHandlerTest {

    private lateinit var orderRepository: InMemoryOrderRepository
    private lateinit var sageRepository: InMemorySageRepository
    private lateinit var outboxRepository: InMemoryOutboxRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var handler: ReservationConfirmHandler

    @BeforeEach
    fun setUp() {
        orderRepository = InMemoryOrderRepository()
        sageRepository = InMemorySageRepository()
        outboxRepository = InMemoryOutboxRepository()
        objectMapper = ObjectMapper()
        handler = ReservationConfirmHandler(
            orderRepository,
            sageRepository,
            outboxRepository,
            objectMapper
        )
    }

    @Test
    fun `예약 확정 성공 - saga가 업데이트되고 결제 확정 요청 outbox가 생성된다`() {
        // given
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
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
        saga.paymentId = paymentId
        sageRepository.save(saga)

        val event = OrderInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_CONFIRM,
            payload = ReservationConfirmSuccessPayload(
                reservationId = reservationId,
                reservationItemInfoList = listOf(
                    InboundReservationItem(
                        productId = UUID.randomUUID(),
                        qty = 2
                    )
                )
            )
        )

        // when
        handler.handle(event)

        // then
        val updatedSaga = sageRepository.findByOrderId(orderId)
        assertNotNull(updatedSaga)
        assertNotNull(updatedSaga.reservationReservedAt)

        val outboxRecords = outboxRepository.findAll()
        assertEquals(1, outboxRecords.size)
        val outboxRecord = outboxRecords.first()
        assertEquals(orderId, outboxRecord.orderId)
        assertEquals(eventId, outboxRecord.idempotencyKey)
        assertEquals(OutboundEventType.PAYMENT_CONFIRM_REQUEST, outboxRecord.eventType)
    }

    @Test
    fun `예약 확정 성공 - saga에 paymentId가 없으면 예외가 발생한다`() {
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
        // paymentId를 설정하지 않음
        sageRepository.save(saga)

        val event = OrderInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_CONFIRM,
            payload = ReservationConfirmSuccessPayload(
                reservationId = reservationId,
                reservationItemInfoList = listOf(
                    InboundReservationItem(
                        productId = UUID.randomUUID(),
                        qty = 2
                    )
                )
            )
        )

        // when & then
        val exception = kotlin.runCatching { handler.handle(event) }.exceptionOrNull()
        assertNotNull(exception)
        assert(exception is IllegalStateException)
        assert(exception.message!!.contains("Payment ID not found"))
    }
}

