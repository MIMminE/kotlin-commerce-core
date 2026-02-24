package nuts.commerce.orderservice.event.inbound.handler

import nuts.commerce.orderservice.event.inbound.InboundEventType
import nuts.commerce.orderservice.event.inbound.OrderInboundEvent
import nuts.commerce.orderservice.event.inbound.ReservationCreationFailedPayload
import nuts.commerce.orderservice.model.Order
import nuts.commerce.orderservice.model.OrderSaga
import nuts.commerce.orderservice.model.OrderStatus
import nuts.commerce.orderservice.testutil.InMemoryOrderRepository
import nuts.commerce.orderservice.testutil.InMemorySageRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("NonAsciiCharacters")
class ReservationCreateFailHandlerTest {

    private lateinit var orderRepository: InMemoryOrderRepository
    private lateinit var sageRepository: InMemorySageRepository
    private lateinit var handler: ReservationCreateFailHandler

    @BeforeEach
    fun setUp() {
        orderRepository = InMemoryOrderRepository()
        sageRepository = InMemorySageRepository()
        handler = ReservationCreateFailHandler(
            orderRepository,
            sageRepository
        )
    }

    @Test
    fun `예약 생성 실패 - 주문 상태가 FAIL로 변경되고 saga에 실패가 기록된다`() {
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
            eventType = InboundEventType.RESERVATION_CREATION_FAILED,
            payload = ReservationCreationFailedPayload(
                reason = "out of stock"
            )
        )

        // when
        handler.handle(event)

        // then
        val updatedOrder = orderRepository.findById(orderId)
        assertNotNull(updatedOrder)
        assertEquals(OrderStatus.FAIL, updatedOrder.status)

        val updatedSaga = sageRepository.findByOrderId(orderId)
        assertNotNull(updatedSaga)
        assertNotNull(updatedSaga.failedAt)
    }
}

