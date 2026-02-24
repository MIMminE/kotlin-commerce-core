package nuts.commerce.paymentservice.adapter.system

import nuts.commerce.paymentservice.event.inbound.InboundEventType
import nuts.commerce.paymentservice.event.inbound.PaymentConfirmPayload
import nuts.commerce.paymentservice.event.inbound.PaymentCreatePayload
import nuts.commerce.paymentservice.event.inbound.PaymentInboundEvent
import nuts.commerce.paymentservice.event.inbound.PaymentReleasePayload
import nuts.commerce.paymentservice.event.inbound.handler.PaymentConfirmRequestHandler
import nuts.commerce.paymentservice.event.inbound.handler.PaymentCreateRequestHandler
import nuts.commerce.paymentservice.event.inbound.handler.PaymentEventHandler
import nuts.commerce.paymentservice.event.inbound.handler.PaymentReleaseRequestHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
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
    classes = [KafkaEventListener::class, TestPaymentHandlerConfig::class]
)
@ImportAutoConfiguration(KafkaAutoConfiguration::class)
class KafkaEventListenerTest {

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, PaymentInboundEvent>

    @Autowired
    @field:Qualifier("paymentCreateHandler")
    lateinit var createHandler: PaymentEventHandler

    @Autowired
    @field:Qualifier("paymentConfirmHandler")
    lateinit var confirmHandler: PaymentEventHandler

    @Autowired
    @field:Qualifier("paymentReleaseHandler")
    lateinit var releaseHandler: PaymentEventHandler

    companion object {
        @Container
        @ServiceConnection
        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"))
    }

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(createHandler, confirmHandler, releaseHandler)
        whenever(createHandler.supportType).thenReturn(InboundEventType.PAYMENT_CREATE_REQUEST)
        whenever(confirmHandler.supportType).thenReturn(InboundEventType.PAYMENT_CONFIRM_REQUEST)
        whenever(releaseHandler.supportType).thenReturn(InboundEventType.PAYMENT_RELEASE_REQUEST)
    }

    @Test
    fun `PAYMENT_CREATE_REQUEST 이벤트는 createHandler로 전달된다`() {
        // given
        val orderId = UUID.randomUUID()
        val event = PaymentInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = orderId,
            eventType = InboundEventType.PAYMENT_CREATE_REQUEST,
            payload = PaymentCreatePayload(amount = 10000L, currency = "KRW")
        )

        // when
        kafkaTemplate.send("payment-event", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        // then
        verify(createHandler, timeout(10_000)).handle(any())
        verify(confirmHandler, never()).handle(any())
        verify(releaseHandler, never()).handle(any())
    }


    @Test
    fun `PAYMENT_CONFIRM 이벤트는 confirmHandler로 전달된다`() {
        // given
        val orderId = UUID.randomUUID()
        val event = PaymentInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = orderId,
            eventType = InboundEventType.PAYMENT_CONFIRM_REQUEST,
            payload = PaymentConfirmPayload(
                paymentId = UUID.randomUUID()
            )
        )

        // when
        kafkaTemplate.send("payment-event", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        // then
        verify(confirmHandler, timeout(10_000)).handle(any())
        verify(createHandler, never()).handle(any())
        verify(releaseHandler, never()).handle(any())
    }


    @Test
    fun `PAYMENT_RELEASE 이벤트는 releaseHandler로 전달된다`() {
        // given
        val orderId = UUID.randomUUID()
        val event = PaymentInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = orderId,
            eventType = InboundEventType.PAYMENT_RELEASE_REQUEST,
            payload = PaymentReleasePayload(
                paymentId = UUID.randomUUID()
            )
        )

        // when
        kafkaTemplate.send("payment-event", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        // then
        verify(releaseHandler, timeout(10_000)).handle(any())
        verify(createHandler, never()).handle(any())
        verify(confirmHandler, never()).handle(any())
    }


    @Test
    fun `여러 이벤트를 비동기로 전송할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val events = listOf(
            PaymentInboundEvent(
                eventId = UUID.randomUUID(),
                orderId = orderId,
                eventType = InboundEventType.PAYMENT_CREATE_REQUEST,
                payload = PaymentCreatePayload(amount = 10000L, currency = "KRW")
            ),
            PaymentInboundEvent(
                eventId = UUID.randomUUID(),
                orderId = orderId,
                eventType = InboundEventType.PAYMENT_CONFIRM_REQUEST,
                payload = PaymentConfirmPayload(
                    paymentId = UUID.randomUUID()
                )
            ),
            PaymentInboundEvent(
                eventId = UUID.randomUUID(),
                orderId = orderId,
                eventType = InboundEventType.PAYMENT_RELEASE_REQUEST,
                payload = PaymentReleasePayload(
                    paymentId = UUID.randomUUID()
                )
            )
        )

        // when
        events.forEach { event ->
            kafkaTemplate.send("payment-event", event).get(5, TimeUnit.SECONDS)
        }
        kafkaTemplate.flush()

        // then
        verify(createHandler, timeout(10_000)).handle(any())
        verify(confirmHandler, timeout(10_000)).handle(any())
        verify(releaseHandler, timeout(10_000)).handle(any())
    }
}

@TestConfiguration(proxyBeanMethods = false)
class TestPaymentHandlerConfig {

    @Bean("paymentCreateHandler")
    fun paymentCreateHandler(): PaymentCreateRequestHandler {
        val handler = mock<PaymentCreateRequestHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.PAYMENT_CREATE_REQUEST)
        return handler
    }

    @Bean("paymentConfirmHandler")
    fun paymentConfirmHandler(): PaymentConfirmRequestHandler {
        val handler = mock<PaymentConfirmRequestHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.PAYMENT_CONFIRM_REQUEST)
        return handler
    }

    @Bean("paymentReleaseHandler")
    fun paymentReleaseHandler(): PaymentReleaseRequestHandler {
        val handler = mock<PaymentReleaseRequestHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.PAYMENT_RELEASE_REQUEST)
        return handler
    }
}