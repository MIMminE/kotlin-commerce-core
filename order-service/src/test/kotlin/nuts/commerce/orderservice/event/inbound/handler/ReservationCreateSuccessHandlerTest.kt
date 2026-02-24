package nuts.commerce.orderservice.event.inbound.handler

import nuts.commerce.orderservice.event.inbound.InboundEventType
import nuts.commerce.orderservice.event.inbound.InboundReservationItem
import nuts.commerce.orderservice.event.inbound.OrderInboundEvent
import nuts.commerce.orderservice.event.inbound.ReservationCreationSucceededPayload
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
class ReservationCreateSuccessHandlerTest {

    private lateinit var orderRepository: InMemoryOrderRepository
    private lateinit var sageRepository: InMemorySageRepository
    private lateinit var outboxRepository: InMemoryOutboxRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var handler: ReservationCreateSuccessHandler

    @BeforeEach
    fun setUp() {
        orderRepository = InMemoryOrderRepository()
        sageRepository = InMemorySageRepository()
        outboxRepository = InMemoryOutboxRepository()
        objectMapper = ObjectMapper()
        handler = ReservationCreateSuccessHandler(
            orderRepository,
            sageRepository,
            outboxRepository,
            objectMapper
        )
    }

    @Test
    fun `예약 생성 성공 - 주문 상태가 PAYING으로 변경되고 결제 생성 요청 outbox가 생성된다`() {
        // given
        val orderId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        val order = Order.create(
            orderId = orderId,
            idempotencyKey = UUID.randomUUID(),
            userId = "user123",
            status = OrderStatus.CREATED
        )
        orderRepository.save(order)

        val saga = OrderSaga.create(
            orderId = orderId,
            totalPrice = 10000,
            currency = "KRW",
            reservationRequestedAt = Instant.now()
        )
        sageRepository.save(saga)

        val event = OrderInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = ReservationCreationSucceededPayload(
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
        val updatedOrder = orderRepository.findById(orderId)
        assertNotNull(updatedOrder)
        assertEquals(OrderStatus.PAYING, updatedOrder.status)

        val updatedSaga = sageRepository.findByOrderId(orderId)
        assertNotNull(updatedSaga)
        assertNotNull(updatedSaga.paymentRequestedAt)

        val outboxRecords = outboxRepository.findAll()
        assertEquals(1, outboxRecords.size)
        val outboxRecord = outboxRecords.first()
        assertEquals(orderId, outboxRecord.orderId)
        assertEquals(eventId, outboxRecord.idempotencyKey)
        assertEquals(OutboundEventType.PAYMENT_CREATE_REQUEST, outboxRecord.eventType)
    }

    @Test
    fun `예약 생성 성공 - saga가 존재하지 않으면 예외가 발생한다`() {
        // given
        val orderId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        val order = Order.create(
            orderId = orderId,
            idempotencyKey = UUID.randomUUID(),
            userId = "user123",
            status = OrderStatus.CREATED
        )
        orderRepository.save(order)
        // saga를 저장하지 않음

        val event = OrderInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = ReservationCreationSucceededPayload(
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
        assert(exception.message!!.contains("Sage record not found"))
    }
}

