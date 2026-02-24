package nuts.commerce.orderservice.adapter.message

import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.OutboundReservationItem
import nuts.commerce.orderservice.event.outbound.ReservationConfirmPayloadReservation
import nuts.commerce.orderservice.event.outbound.ReservationCreatePayloadReservation
import nuts.commerce.orderservice.event.outbound.ReservationOutboundEvent
import nuts.commerce.orderservice.event.outbound.ReservationReleasePayloadReservation
import nuts.commerce.orderservice.port.message.ProduceResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("NonAsciiCharacters")
@SpringBootTest(
    classes = [KafkaReservationEventProducer::class]
)
@ImportAutoConfiguration(KafkaAutoConfiguration::class)
class KafkaReservationEventProducerTest {

    @Autowired
    lateinit var producer: KafkaReservationEventProducer

    companion object {
        @Container
        @ServiceConnection
        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"))
    }

    @Test
    fun `예약 이벤트를 성공적으로 프로듀스할 수 있다`() {
        // given
        val eventId = UUID.randomUUID()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()

        val event = ReservationOutboundEvent(
            eventId = eventId,
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.RESERVATION_CREATE_REQUEST,
            payload = ReservationCreatePayloadReservation(
                reservationItems = listOf(
                    OutboundReservationItem(
                        productId = UUID.randomUUID(),
                        price = 10000,
                        currency = "KRW",
                        qty = 2
                    )
                )
            )
        )

        // when
        val resultFuture = producer.produce(event)
        val result = resultFuture.get(10, TimeUnit.SECONDS)

        // then
        assertNotNull(result)
        assertTrue(result is ProduceResult.Success)
        val successResult = result as ProduceResult.Success
        assertEquals(eventId, successResult.eventId)
        assertEquals(outboxId, successResult.outboxId)
    }

    @Test
    fun `서로 다른 예약 이벤트 타입을 프로듀스할 수 있다`() {
        // given
        val eventTypes = listOf(
            OutboundEventType.RESERVATION_CREATE_REQUEST,
            OutboundEventType.RESERVATION_CONFIRM_REQUEST,
            OutboundEventType.RESERVATION_RELEASE_REQUEST
        )

        // when & then
        eventTypes.forEach { eventType ->
            val reservationId = UUID.randomUUID()
            val event = ReservationOutboundEvent(
                eventId = UUID.randomUUID(),
                outboxId = UUID.randomUUID(),
                orderId = UUID.randomUUID(),
                eventType = eventType,
                payload = when (eventType) {
                    OutboundEventType.RESERVATION_CREATE_REQUEST -> ReservationCreatePayloadReservation(
                        reservationItems = listOf(
                            OutboundReservationItem(
                                productId = UUID.randomUUID(),
                                price = 10000,
                                currency = "KRW",
                                qty = 1
                            )
                        )
                    )

                    OutboundEventType.RESERVATION_CONFIRM_REQUEST -> ReservationConfirmPayloadReservation(
                        reservationId = reservationId
                    )

                    OutboundEventType.RESERVATION_RELEASE_REQUEST -> ReservationReleasePayloadReservation(
                        reservationId = reservationId
                    )

                    else -> throw IllegalArgumentException("Unsupported event type")
                }
            )

            val result = producer.produce(event)
                .get(10, TimeUnit.SECONDS)

            assertNotNull(result)
            assertTrue(result is ProduceResult.Success)
        }
    }

    @Test
    fun `비동기 프로듀스 결과를 올바르게 처리한다`() {
        // given
        val events = (1..10).map {
            ReservationOutboundEvent(
                eventId = UUID.randomUUID(),
                outboxId = UUID.randomUUID(),
                orderId = UUID.randomUUID(),
                eventType = OutboundEventType.RESERVATION_CREATE_REQUEST,
                payload = ReservationCreatePayloadReservation(
                    reservationItems = listOf(
                        OutboundReservationItem(
                            productId = UUID.randomUUID(),
                            price = (it * 1000).toLong(),
                            currency = "KRW",
                            qty = 1
                        )
                    )
                )
            )
        }

        // when - 비동기 프로듀스
        val futures = events.map { producer.produce(it) }

        // then - 모든 결과 확인
        futures.forEach { future ->
            val result = future.get(10, TimeUnit.SECONDS)
            assertNotNull(result)
            assertTrue(result is ProduceResult.Success)
        }
    }

    @Test
    fun `예약 생성 요청 이벤트를 올바른 페이로드와 함께 프로듀스할 수 있다`() {
        // given
        val eventId = UUID.randomUUID()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val price = 50000L
        val currency = "KRW"
        val qty = 3L

        val event = ReservationOutboundEvent(
            eventId = eventId,
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.RESERVATION_CREATE_REQUEST,
            payload = ReservationCreatePayloadReservation(
                reservationItems = listOf(
                    OutboundReservationItem(
                        productId = productId,
                        price = price,
                        currency = currency,
                        qty = qty
                    )
                )
            )
        )

        // when
        val result = producer.produce(event)
            .get(10, TimeUnit.SECONDS)

        // then
        assertNotNull(result)
        assertTrue(result is ProduceResult.Success)
        val successResult = result as ProduceResult.Success
        assertEquals(eventId, successResult.eventId)
        assertEquals(outboxId, successResult.outboxId)

        val payload = event.payload as ReservationCreatePayloadReservation
        assertEquals(1, payload.reservationItems.size)
        val item = payload.reservationItems[0]
        assertEquals(productId, item.productId)
        assertEquals(price, item.price)
        assertEquals(currency, item.currency)
        assertEquals(qty, item.qty)
    }

    @Test
    fun `예약 확정 요청 이벤트를 올바른 페이로드와 함께 프로듀스할 수 있다`() {
        // given
        val eventId = UUID.randomUUID()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()

        val event = ReservationOutboundEvent(
            eventId = eventId,
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.RESERVATION_CONFIRM_REQUEST,
            payload = ReservationConfirmPayloadReservation(
                reservationId = reservationId
            )
        )

        // when
        val result = producer.produce(event)
            .get(10, TimeUnit.SECONDS)

        // then
        assertNotNull(result)
        assertTrue(result is ProduceResult.Success)
        val successResult = result as ProduceResult.Success
        assertEquals(eventId, successResult.eventId)
        assertEquals(outboxId, successResult.outboxId)

        val payload = event.payload as ReservationConfirmPayloadReservation
        assertEquals(reservationId, payload.reservationId)
    }

    @Test
    fun `예약 해제 요청 이벤트를 올바른 페이로드와 함께 프로듀스할 수 있다`() {
        // given
        val eventId = UUID.randomUUID()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()

        val event = ReservationOutboundEvent(
            eventId = eventId,
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.RESERVATION_RELEASE_REQUEST,
            payload = ReservationReleasePayloadReservation(
                reservationId = reservationId
            )
        )

        // when
        val result = producer.produce(event)
            .get(10, TimeUnit.SECONDS)

        // then
        assertNotNull(result)
        assertTrue(result is ProduceResult.Success)
        val successResult = result as ProduceResult.Success
        assertEquals(eventId, successResult.eventId)
        assertEquals(outboxId, successResult.outboxId)

        val payload = event.payload as ReservationReleasePayloadReservation
        assertEquals(reservationId, payload.reservationId)
    }


    @Test
    fun `각 이벤트가 고유한 eventId를 가진다`() {
        // given
        val events = (1..10).map {
            ReservationOutboundEvent(
                eventId = UUID.randomUUID(),  // 각각 고유한 UUID
                outboxId = UUID.randomUUID(),
                orderId = UUID.randomUUID(),
                eventType = OutboundEventType.RESERVATION_CREATE_REQUEST,
                payload = ReservationCreatePayloadReservation(
                    reservationItems = listOf(
                        OutboundReservationItem(
                            productId = UUID.randomUUID(),
                            price = 10000,
                            currency = "KRW",
                            qty = 1
                        )
                    )
                )
            )
        }

        // when
        val results = events.map { producer.produce(it).get(10, TimeUnit.SECONDS) }

        // then
        assertTrue(results.all { it is ProduceResult.Success })
        val eventIds = events.map { it.eventId }
        assertEquals(eventIds.size, eventIds.distinct().size)  // 모든 eventId가 고유함
    }

    @Test
    fun `각 이벤트가 고유한 outboxId를 가진다`() {
        // given
        val events = (1..10).map {
            ReservationOutboundEvent(
                eventId = UUID.randomUUID(),
                outboxId = UUID.randomUUID(),  // 각각 고유한 UUID
                orderId = UUID.randomUUID(),
                eventType = OutboundEventType.RESERVATION_CREATE_REQUEST,
                payload = ReservationCreatePayloadReservation(
                    reservationItems = listOf(
                        OutboundReservationItem(
                            productId = UUID.randomUUID(),
                            price = 10000,
                            currency = "KRW",
                            qty = 1
                        )
                    )
                )
            )
        }

        // when
        val results = events.map { producer.produce(it).get(10, TimeUnit.SECONDS) }

        // then
        assertTrue(results.all { it is ProduceResult.Success })
        val successResults = results.filterIsInstance<ProduceResult.Success>()
        val outboxIds = successResults.map { it.outboxId }
        assertEquals(outboxIds.size, outboxIds.distinct().size)  // 모든 outboxId가 고유함
    }


}