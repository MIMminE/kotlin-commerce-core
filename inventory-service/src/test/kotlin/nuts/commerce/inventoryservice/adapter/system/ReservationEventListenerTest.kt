package nuts.commerce.inventoryservice.adapter.system

import nuts.commerce.inventoryservice.event.inbound.InboundEventType
import nuts.commerce.inventoryservice.event.inbound.InboundReservationItem
import nuts.commerce.inventoryservice.event.inbound.ReservationConfirmPayload
import nuts.commerce.inventoryservice.event.inbound.ReservationCreatePayload
import nuts.commerce.inventoryservice.event.inbound.ReservationInboundEvent
import nuts.commerce.inventoryservice.event.inbound.ReservationReleasePayload
import nuts.commerce.inventoryservice.event.inbound.handler.ReservationConfirmHandler
import nuts.commerce.inventoryservice.event.inbound.handler.ReservationCreateHandler
import nuts.commerce.inventoryservice.event.inbound.handler.ReservationEventHandler
import nuts.commerce.inventoryservice.event.inbound.handler.ReservationReleaseHandler
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
    classes = [ReservationEventListener::class, TestReservationHandlerConfig::class]
)
@ImportAutoConfiguration(KafkaAutoConfiguration::class)
class ReservationEventListenerTest {

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, ReservationInboundEvent>

    @Autowired
    @field:Qualifier("reservationCreateHandler")
    lateinit var createHandler: ReservationEventHandler

    @Autowired
    @field:Qualifier("reservationConfirmHandler")
    lateinit var confirmHandler: ReservationEventHandler

    @Autowired
    @field:Qualifier("reservationReleaseHandler")
    lateinit var releaseHandler: ReservationEventHandler

    companion object {
        @Container
        @ServiceConnection
        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"))
    }

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(createHandler, confirmHandler, releaseHandler)
        whenever(createHandler.supportType).thenReturn(InboundEventType.RESERVATION_CREATE_REQUEST)
        whenever(confirmHandler.supportType).thenReturn(InboundEventType.RESERVATION_CONFIRM_REQUEST)
        whenever(releaseHandler.supportType).thenReturn(InboundEventType.RESERVATION_RELEASE_REQUEST)
    }

    @Test
    fun `RESERVATION_CREATE 이벤트는 createHandler로 전달된다`() {
        // given
        val orderId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val event = ReservationInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_CREATE_REQUEST,
            payload = ReservationCreatePayload(
                requestItem = listOf(
                    InboundReservationItem(productId = productId, qty = 10L)
                )
            )
        )

        // when
        kafkaTemplate.send("reservation-event", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        // then
        verify(createHandler, timeout(10_000)).handle(any())
        verify(confirmHandler, never()).handle(any())
        verify(releaseHandler, never()).handle(any())
    }

    @Test
    fun `RESERVATION_CONFIRM 이벤트는 confirmHandler로 전달된다`() {
        // given
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val event = ReservationInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_CONFIRM_REQUEST,
            payload = ReservationConfirmPayload(
                reservationId = reservationId
            )
        )

        // when
        kafkaTemplate.send("reservation-event", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        // then
        verify(confirmHandler, timeout(10_000)).handle(any())
        verify(createHandler, never()).handle(any())
        verify(releaseHandler, never()).handle(any())
    }

    @Test
    fun `RESERVATION_RELEASE 이벤트는 releaseHandler로 전달된다`() {
        // given
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val event = ReservationInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_RELEASE_REQUEST,
            payload = ReservationReleasePayload(
                reservationId = reservationId
            )
        )

        // when
        kafkaTemplate.send("reservation-event", event).get(5, TimeUnit.SECONDS)
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
        val reservationId = UUID.randomUUID()
        val productId = UUID.randomUUID()

        val events = listOf(
            ReservationInboundEvent(
                eventId = UUID.randomUUID(),
                orderId = orderId,
                eventType = InboundEventType.RESERVATION_CREATE_REQUEST,
                payload = ReservationCreatePayload(
                    requestItem = listOf(
                        InboundReservationItem(productId = productId, qty = 10L)
                    )
                )
            ),
            ReservationInboundEvent(
                eventId = UUID.randomUUID(),
                orderId = orderId,
                eventType = InboundEventType.RESERVATION_CONFIRM_REQUEST,
                payload = ReservationConfirmPayload(
                    reservationId = reservationId
                )
            ),
            ReservationInboundEvent(
                eventId = UUID.randomUUID(),
                orderId = orderId,
                eventType = InboundEventType.RESERVATION_RELEASE_REQUEST,
                payload = ReservationReleasePayload(
                    reservationId = reservationId
                )
            )
        )

        // when
        events.forEach { event ->
            kafkaTemplate.send("reservation-event", event).get(5, TimeUnit.SECONDS)
        }
        kafkaTemplate.flush()

        // then
        verify(createHandler, timeout(10_000)).handle(any())
        verify(confirmHandler, timeout(10_000)).handle(any())
        verify(releaseHandler, timeout(10_000)).handle(any())
    }

    @Test
    fun `다중 상품 예약 생성 이벤트를 처리할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val products = (1..3).map { UUID.randomUUID() }

        val event = ReservationInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_CREATE_REQUEST,
            payload = ReservationCreatePayload(
                requestItem = products.mapIndexed { index, productId ->
                    InboundReservationItem(productId = productId, qty = ((index + 1) * 10).toLong())
                }
            )
        )

        // when
        kafkaTemplate.send("reservation-event", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        // then
        verify(createHandler, timeout(10_000)).handle(any())
        verify(confirmHandler, never()).handle(any())
        verify(releaseHandler, never()).handle(any())
    }

    @Test
    fun `같은 orderId에 대한 연속적인 이벤트를 처리할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val productId = UUID.randomUUID()

        val createEvent = ReservationInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_CREATE_REQUEST,
            payload = ReservationCreatePayload(
                requestItem = listOf(
                    InboundReservationItem(productId = productId, qty = 5L)
                )
            )
        )

        val confirmEvent = ReservationInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_CONFIRM_REQUEST,
            payload = ReservationConfirmPayload(
                reservationId = UUID.randomUUID()
            )
        )

        // when
        kafkaTemplate.send("reservation-event", createEvent).get(5, TimeUnit.SECONDS)
        kafkaTemplate.send("reservation-event", confirmEvent).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        // then
        verify(createHandler, timeout(10_000)).handle(any())
        verify(confirmHandler, timeout(10_000)).handle(any())
        verify(releaseHandler, never()).handle(any())
    }

    @Test
    fun `대량의 이벤트를 병렬로 처리할 수 있다`() {
        // given
        val events = (1..10).map {
            ReservationInboundEvent(
                eventId = UUID.randomUUID(),
                orderId = UUID.randomUUID(),
                eventType = InboundEventType.RESERVATION_CREATE_REQUEST,
                payload = ReservationCreatePayload(
                    requestItem = listOf(
                        InboundReservationItem(productId = UUID.randomUUID(), qty = (it * 5).toLong())
                    )
                )
            )
        }

        // when
        events.forEach { event ->
            kafkaTemplate.send("reservation-event", event).get(5, TimeUnit.SECONDS)
        }
        kafkaTemplate.flush()

        // then
        verify(createHandler, timeout(15_000).times(10)).handle(any())
    }

    @Test
    fun `서로 다른 타입의 이벤트가 올바른 핸들러로 라우팅된다`() {
        // given
        val createEvent = ReservationInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = UUID.randomUUID(),
            eventType = InboundEventType.RESERVATION_CREATE_REQUEST,
            payload = ReservationCreatePayload(
                requestItem = listOf(
                    InboundReservationItem(productId = UUID.randomUUID(), qty = 10L)
                )
            )
        )

        val confirmEvent = ReservationInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = UUID.randomUUID(),
            eventType = InboundEventType.RESERVATION_CONFIRM_REQUEST,
            payload = ReservationConfirmPayload(
                reservationId = UUID.randomUUID()
            )
        )

        val releaseEvent = ReservationInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = UUID.randomUUID(),
            eventType = InboundEventType.RESERVATION_RELEASE_REQUEST,
            payload = ReservationReleasePayload(
                reservationId = UUID.randomUUID()
            )
        )

        // when
        kafkaTemplate.send("reservation-event", createEvent).get(5, TimeUnit.SECONDS)
        kafkaTemplate.send("reservation-event", confirmEvent).get(5, TimeUnit.SECONDS)
        kafkaTemplate.send("reservation-event", releaseEvent).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        // then
        verify(createHandler, timeout(10_000)).handle(any())
        verify(confirmHandler, timeout(10_000)).handle(any())
        verify(releaseHandler, timeout(10_000)).handle(any())
    }
}

@TestConfiguration(proxyBeanMethods = false)
class TestReservationHandlerConfig {

    @Bean("reservationCreateHandler")
    fun reservationCreateHandler(): ReservationCreateHandler {
        val handler = mock<ReservationCreateHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.RESERVATION_CREATE_REQUEST)
        return handler
    }

    @Bean("reservationConfirmHandler")
    fun reservationConfirmHandler(): ReservationConfirmHandler {
        val handler = mock<ReservationConfirmHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.RESERVATION_CONFIRM_REQUEST)
        return handler
    }

    @Bean("reservationReleaseHandler")
    fun reservationReleaseHandler(): ReservationReleaseHandler {
        val handler = mock<ReservationReleaseHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.RESERVATION_RELEASE_REQUEST)
        return handler
    }
}

