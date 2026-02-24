package nuts.commerce.orderservice.event.inbound.handler

import nuts.commerce.orderservice.event.inbound.InboundEventType
import nuts.commerce.orderservice.event.inbound.InboundReservationItem
import nuts.commerce.orderservice.event.inbound.OrderInboundEvent
import nuts.commerce.orderservice.event.inbound.ReservationReleaseSuccessPayload
import nuts.commerce.orderservice.model.OrderSaga
import nuts.commerce.orderservice.testutil.InMemorySageRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import kotlin.test.assertNotNull

@Suppress("NonAsciiCharacters")
class ReservationReleaseHandlerTest {

    private lateinit var sageRepository: InMemorySageRepository
    private lateinit var handler: ReservationReleaseHandler

    @BeforeEach
    fun setUp() {
        sageRepository = InMemorySageRepository()
        handler = ReservationReleaseHandler(sageRepository)
    }

    @Test
    fun `예약 해제 성공 - saga에 예약 해제 정보가 기록된다`() {
        // given
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

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
            eventType = InboundEventType.RESERVATION_RELEASE,
            payload = ReservationReleaseSuccessPayload(
                reservationId = reservationId,
                reservationItemInfoList = listOf(
                    InboundReservationItem(
                        productId = UUID.randomUUID(),
                        qty = 2
                    )
                ),
                reason = "order cancelled"
            )
        )

        // when
        handler.handle(event)

        // then
        val updatedSaga = sageRepository.findByOrderId(orderId)
        assertNotNull(updatedSaga)
        assertNotNull(updatedSaga.reservationReleasedAt)
    }
}

