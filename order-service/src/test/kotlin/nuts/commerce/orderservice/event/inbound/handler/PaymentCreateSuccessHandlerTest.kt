package nuts.commerce.orderservice.event.inbound.handler

import nuts.commerce.orderservice.event.inbound.InboundEventType
import nuts.commerce.orderservice.event.inbound.OrderInboundEvent
import nuts.commerce.orderservice.event.inbound.PaymentCreationSuccessPayload
import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.model.OrderSaga
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
class PaymentCreateSuccessHandlerTest {

    private lateinit var sageRepository: InMemorySageRepository
    private lateinit var outboxRepository: InMemoryOutboxRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var handler: PaymentCreateSuccessHandler

    @BeforeEach
    fun setUp() {
        sageRepository = InMemorySageRepository()
        outboxRepository = InMemoryOutboxRepository()
        objectMapper = ObjectMapper()
        handler = PaymentCreateSuccessHandler(
            sageRepository,
            outboxRepository,
            objectMapper
        )
    }

    @Test
    fun `결제 생성 성공 - saga가 업데이트되고 예약 확정 요청 outbox가 생성된다`() {
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
            eventType = InboundEventType.PAYMENT_CREATION_SUCCEEDED,
            payload = PaymentCreationSuccessPayload(
                paymentProvider = "toss"
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
        assertEquals(OutboundEventType.RESERVATION_CONFIRM_REQUEST, outboxRecord.eventType)
    }

    @Test
    fun `결제 생성 성공 - saga에 reservationId가 없으면 예외가 발생한다`() {
        // given
        val orderId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        val saga = OrderSaga.create(
            orderId = orderId,
            totalPrice = 10000,
            currency = "KRW",
            reservationRequestedAt = Instant.now()
        )
        // reservationId를 설정하지 않음
        sageRepository.save(saga)

        val event = OrderInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.PAYMENT_CREATION_SUCCEEDED,
            payload = PaymentCreationSuccessPayload(
                paymentProvider = "toss"
            )
        )

        // when & then
        val exception = kotlin.runCatching { handler.handle(event) }.exceptionOrNull()
        assertNotNull(exception)
        assert(exception is IllegalStateException)
        assert(exception.message!!.contains("reservation id is null"))
    }
}

