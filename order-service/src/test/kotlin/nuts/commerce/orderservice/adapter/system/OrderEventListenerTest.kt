package nuts.commerce.orderservice.adapter.system

import nuts.commerce.orderservice.event.inbound.InboundEventType
import nuts.commerce.orderservice.event.inbound.OrderInboundEvent
import nuts.commerce.orderservice.event.inbound.ReservationCreationSucceededPayload
import nuts.commerce.orderservice.event.inbound.ReservationCreationFailedPayload
import nuts.commerce.orderservice.event.inbound.ReservationConfirmSuccessPayload
import nuts.commerce.orderservice.event.inbound.ReservationReleaseSuccessPayload
import nuts.commerce.orderservice.event.inbound.PaymentCreationSuccessPayload
import nuts.commerce.orderservice.event.inbound.PaymentCreationFailedPayload
import nuts.commerce.orderservice.event.inbound.PaymentConfirmSuccessPayload
import nuts.commerce.orderservice.event.inbound.PaymentReleaseSuccessPayload
import nuts.commerce.orderservice.event.inbound.InboundReservationItem
import nuts.commerce.orderservice.event.inbound.handler.ReservationCreateSuccessHandler
import nuts.commerce.orderservice.event.inbound.handler.ReservationCreateFailHandler
import nuts.commerce.orderservice.event.inbound.handler.ReservationConfirmHandler
import nuts.commerce.orderservice.event.inbound.handler.ReservationReleaseHandler
import nuts.commerce.orderservice.event.inbound.handler.PaymentCreateSuccessHandler
import nuts.commerce.orderservice.event.inbound.handler.PaymentCreateFailHandler
import nuts.commerce.orderservice.event.inbound.handler.PaymentConfirmHandler
import nuts.commerce.orderservice.event.inbound.handler.PaymentReleaseHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("NonAsciiCharacters")
@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(
    classes = [OrderEventListener::class, TestOrderEventHandlerConfig::class]
)
@ImportAutoConfiguration(KafkaAutoConfiguration::class)
class OrderEventListenerTest {

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, OrderInboundEvent>

    @Autowired
    @field:Qualifier("reservationCreateSuccessHandler")
    lateinit var reservationCreateSuccessHandler: ReservationCreateSuccessHandler

    @Autowired
    @field:Qualifier("reservationCreateFailHandler")
    lateinit var reservationCreateFailHandler: ReservationCreateFailHandler

    @Autowired
    @field:Qualifier("reservationConfirmHandler")
    lateinit var reservationConfirmHandler: ReservationConfirmHandler

    @Autowired
    @field:Qualifier("reservationReleaseHandler")
    lateinit var reservationReleaseHandler: ReservationReleaseHandler

    @Autowired
    @field:Qualifier("paymentCreateSuccessHandler")
    lateinit var paymentCreateSuccessHandler: PaymentCreateSuccessHandler

    @Autowired
    @field:Qualifier("paymentCreateFailHandler")
    lateinit var paymentCreateFailHandler: PaymentCreateFailHandler

    @Autowired
    @field:Qualifier("paymentConfirmHandler")
    lateinit var paymentConfirmHandler: PaymentConfirmHandler

    @Autowired
    @field:Qualifier("paymentReleaseHandler")
    lateinit var paymentReleaseHandler: PaymentReleaseHandler

    companion object {
        @Container
        @ServiceConnection
        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"))
    }

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(
            reservationCreateSuccessHandler,
            reservationCreateFailHandler,
            reservationConfirmHandler,
            reservationReleaseHandler,
            paymentCreateSuccessHandler,
            paymentCreateFailHandler,
            paymentConfirmHandler,
            paymentReleaseHandler
        )

        whenever(reservationCreateSuccessHandler.supportType).thenReturn(InboundEventType.RESERVATION_CREATION_SUCCEEDED)
        whenever(reservationCreateFailHandler.supportType).thenReturn(InboundEventType.RESERVATION_CREATION_FAILED)
        whenever(reservationConfirmHandler.supportType).thenReturn(InboundEventType.RESERVATION_CONFIRM)
        whenever(reservationReleaseHandler.supportType).thenReturn(InboundEventType.RESERVATION_RELEASE)
        whenever(paymentCreateSuccessHandler.supportType).thenReturn(InboundEventType.PAYMENT_CREATION_SUCCEEDED)
        whenever(paymentCreateFailHandler.supportType).thenReturn(InboundEventType.PAYMENT_CREATION_FAILED)
        whenever(paymentConfirmHandler.supportType).thenReturn(InboundEventType.PAYMENT_CONFIRM)
        whenever(paymentReleaseHandler.supportType).thenReturn(InboundEventType.PAYMENT_RELEASE)
    }

    @Test
    fun `RESERVATION_CREATION_SUCCEEDED 이벤트는 reservationCreateSuccessHandler로 전달된다`() {
        // given
        val event = OrderInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = UUID.randomUUID(),
            eventType = InboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = ReservationCreationSucceededPayload(
                reservationItemInfoList = listOf(InboundReservationItem(UUID.randomUUID(), 2))
            )
        )

        // when
        kafkaTemplate.send("order-event-test", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        // then
        verify(reservationCreateSuccessHandler, timeout(10_000)).handle(any())
        verify(reservationCreateFailHandler, never()).handle(any())
        verify(paymentCreateSuccessHandler, never()).handle(any())
    }

    @Test
    fun `RESERVATION_CREATION_FAILED 이벤트는 reservationCreateFailHandler로 전달된다`() {
        // given
        val event = OrderInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = UUID.randomUUID(),
            eventType = InboundEventType.RESERVATION_CREATION_FAILED,
            payload = ReservationCreationFailedPayload(reason = "out of stock")
        )

        // when
        kafkaTemplate.send("order-event-test", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        // then
        verify(reservationCreateFailHandler, timeout(10_000)).handle(any())
        verify(reservationCreateSuccessHandler, never()).handle(any())
        verify(reservationConfirmHandler, never()).handle(any())
    }

    @Test
    fun `RESERVATION_CONFIRM 이벤트는 reservationConfirmHandler로 전달된다`() {
        // given
        val event = OrderInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = UUID.randomUUID(),
            eventType = InboundEventType.RESERVATION_CONFIRM,
            payload = ReservationConfirmSuccessPayload(
                reservationId = UUID.randomUUID(),
                reservationItemInfoList = listOf(InboundReservationItem(UUID.randomUUID(), 1))
            )
        )

        // when
        kafkaTemplate.send("order-event-test", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        // then
        verify(reservationConfirmHandler, timeout(10_000)).handle(any())
        verify(reservationCreateSuccessHandler, never()).handle(any())
        verify(paymentCreateSuccessHandler, never()).handle(any())
    }

    @Test
    fun `RESERVATION_RELEASE 이벤트는 reservationReleaseHandler로 전달된다`() {
        // given
        val event = OrderInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = UUID.randomUUID(),
            eventType = InboundEventType.RESERVATION_RELEASE,
            payload = ReservationReleaseSuccessPayload(
                reservationId = UUID.randomUUID(),
                reservationItemInfoList = listOf(InboundReservationItem(UUID.randomUUID(), 1)),
                reason = "cancelled"
            )
        )

        // when
        kafkaTemplate.send("order-event-test", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        // then
        verify(reservationReleaseHandler, timeout(10_000)).handle(any())
        verify(reservationCreateSuccessHandler, never()).handle(any())
        verify(paymentCreateSuccessHandler, never()).handle(any())
    }

    @Test
    fun `PAYMENT_CREATION_SUCCEEDED 이벤트는 paymentCreateSuccessHandler로 전달된다`() {
        // given
        val event = OrderInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = UUID.randomUUID(),
            eventType = InboundEventType.PAYMENT_CREATION_SUCCEEDED,
            payload = PaymentCreationSuccessPayload(paymentProvider = "credit-card")
        )

        // when
        kafkaTemplate.send("order-event-test", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        // then
        verify(paymentCreateSuccessHandler, timeout(10_000)).handle(any())
        verify(reservationCreateSuccessHandler, never()).handle(any())
        verify(paymentCreateFailHandler, never()).handle(any())
    }

    @Test
    fun `PAYMENT_CREATION_FAILED 이벤트는 paymentCreateFailHandler로 전달된다`() {
        // given
        val event = OrderInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = UUID.randomUUID(),
            eventType = InboundEventType.PAYMENT_CREATION_FAILED,
            payload = PaymentCreationFailedPayload(reason = "insufficient funds")
        )

        // when
        kafkaTemplate.send("order-event-test", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        // then
        verify(paymentCreateFailHandler, timeout(10_000)).handle(any())
        verify(paymentCreateSuccessHandler, never()).handle(any())
        verify(reservationCreateSuccessHandler, never()).handle(any())
    }

    @Test
    fun `PAYMENT_CONFIRM 이벤트는 paymentConfirmHandler로 전달된다`() {
        // given
        val event = OrderInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = UUID.randomUUID(),
            eventType = InboundEventType.PAYMENT_CONFIRM,
            payload = PaymentConfirmSuccessPayload(
                paymentProvider = "credit-card",
                providerPaymentId = UUID.randomUUID()
            )
        )

        // when
        kafkaTemplate.send("order-event-test", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        // then
        verify(paymentConfirmHandler, timeout(10_000)).handle(any())
        verify(paymentCreateSuccessHandler, never()).handle(any())
        verify(reservationCreateSuccessHandler, never()).handle(any())
    }

    @Test
    fun `PAYMENT_RELEASE 이벤트는 paymentReleaseHandler로 전달된다`() {
        // given
        val event = OrderInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = UUID.randomUUID(),
            eventType = InboundEventType.PAYMENT_RELEASE,
            payload = PaymentReleaseSuccessPayload(reason = "order cancelled")
        )

        // when
        kafkaTemplate.send("order-event-test", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        // then
        verify(paymentReleaseHandler, timeout(10_000)).handle(any())
        verify(paymentCreateSuccessHandler, never()).handle(any())
        verify(reservationCreateSuccessHandler, never()).handle(any())
    }
}

@TestConfiguration(proxyBeanMethods = false)
class TestOrderEventHandlerConfig {

    @Bean("reservationCreateSuccessHandler")
    fun reservationCreateSuccessHandler(): ReservationCreateSuccessHandler {
        val handler = mock<ReservationCreateSuccessHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.RESERVATION_CREATION_SUCCEEDED)
        return handler
    }

    @Bean("reservationCreateFailHandler")
    fun reservationCreateFailHandler(): ReservationCreateFailHandler {
        val handler = mock<ReservationCreateFailHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.RESERVATION_CREATION_FAILED)
        return handler
    }

    @Bean("reservationConfirmHandler")
    fun reservationConfirmHandler(): ReservationConfirmHandler {
        val handler = mock<ReservationConfirmHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.RESERVATION_CONFIRM)
        return handler
    }

    @Bean("reservationReleaseHandler")
    fun reservationReleaseHandler(): ReservationReleaseHandler {
        val handler = mock<ReservationReleaseHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.RESERVATION_RELEASE)
        return handler
    }

    @Bean("paymentCreateSuccessHandler")
    fun paymentCreateSuccessHandler(): PaymentCreateSuccessHandler {
        val handler = mock<PaymentCreateSuccessHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.PAYMENT_CREATION_SUCCEEDED)
        return handler
    }

    @Bean("paymentCreateFailHandler")
    fun paymentCreateFailHandler(): PaymentCreateFailHandler {
        val handler = mock<PaymentCreateFailHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.PAYMENT_CREATION_FAILED)
        return handler
    }

    @Bean("paymentConfirmHandler")
    fun paymentConfirmHandler(): PaymentConfirmHandler {
        val handler = mock<PaymentConfirmHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.PAYMENT_CONFIRM)
        return handler
    }

    @Bean("paymentReleaseHandler")
    fun paymentReleaseHandler(): PaymentReleaseHandler {
        val handler = mock<PaymentReleaseHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.PAYMENT_RELEASE)
        return handler
    }
}

