package nuts.commerce.inventoryservice.adapter.message

import nuts.commerce.inventoryservice.event.outbound.OutboundEventType
import nuts.commerce.inventoryservice.event.outbound.ReservationConfirmSuccessPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationCreationFailPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationCreationSuccessPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationOutboundEvent
import nuts.commerce.inventoryservice.event.outbound.ReservationReleaseSuccessPayload
import nuts.commerce.inventoryservice.port.message.ProduceResult
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
import java.util.UUID
import java.util.concurrent.TimeUnit

@Suppress("NonAsciiCharacters")
@SpringBootTest(classes = [KafkaReservationEventProducer::class])
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
    fun `예약 생성 성공 이벤트를 프로듀스할 수 있다`() {
        // given
        val eventId = UUID.randomUUID()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val productId = UUID.randomUUID()

        val event = ReservationOutboundEvent(
            eventId = eventId,
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = ReservationCreationSuccessPayload(
                reservationId = reservationId,
                reservationItemInfoList = listOf(
                    ReservationOutboundEvent.ReservationItem(productId = productId, qty = 10L)
                )
            )
        )

        // when
        val resultFuture = producer.produce(event)
        val result = resultFuture.get(10, TimeUnit.SECONDS)

        // then
        assertNotNull(result)
        assertTrue(result is ProduceResult.Success)
        assertEquals(eventId, (result as ProduceResult.Success).eventId)
        assertEquals(outboxId, result.outboxId)
    }

    @Test
    fun `예약 생성 실패 이벤트를 프로듀스할 수 있다`() {
        // given
        val eventId = UUID.randomUUID()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()

        val event = ReservationOutboundEvent(
            eventId = eventId,
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.RESERVATION_CREATION_FAILED,
            payload = ReservationCreationFailPayload(reason = "재고 부족")
        )

        // when
        val resultFuture = producer.produce(event)
        val result = resultFuture.get(10, TimeUnit.SECONDS)

        // then
        assertNotNull(result)
        assertTrue(result is ProduceResult.Success)
        assertEquals(eventId, (result as ProduceResult.Success).eventId)
    }

    @Test
    fun `예약 확정 이벤트를 프로듀스할 수 있다`() {
        // given
        val eventId = UUID.randomUUID()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()

        val event = ReservationOutboundEvent(
            eventId = eventId,
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.RESERVATION_CONFIRM,
            payload = ReservationConfirmSuccessPayload(
                reservationId = reservationId,
                reservationItemInfoList = listOf(
                    ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 10L)
                )
            )
        )

        // when
        val result = producer.produce(event).get(10, TimeUnit.SECONDS)

        // then
        assertTrue(result is ProduceResult.Success)
        assertEquals(eventId, (result as ProduceResult.Success).eventId)
    }

    @Test
    fun `예약 해제 이벤트를 프로듀스할 수 있다`() {
        // given
        val eventId = UUID.randomUUID()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()

        val event = ReservationOutboundEvent(
            eventId = eventId,
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.RESERVATION_RELEASE,
            payload = ReservationReleaseSuccessPayload(
                reservationId = reservationId,
                reservationItemInfoList = listOf(
                    ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 10L)
                )
            )
        )

        // when
        val result = producer.produce(event).get(10, TimeUnit.SECONDS)

        // then
        assertTrue(result is ProduceResult.Success)
    }


    @Test
    fun `서로 다른 이벤트 타입을 프로듀스할 수 있다`() {
        // given
        val eventTypes = listOf(
            OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            OutboundEventType.RESERVATION_CREATION_FAILED,
            OutboundEventType.RESERVATION_CONFIRM,
            OutboundEventType.RESERVATION_RELEASE
        )

        // when & then
        eventTypes.forEach { eventType ->
            val payload = when (eventType) {
                OutboundEventType.RESERVATION_CREATION_SUCCEEDED -> ReservationCreationSuccessPayload(
                    UUID.randomUUID(),
                    listOf(ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 10L))
                )

                OutboundEventType.RESERVATION_CREATION_FAILED -> ReservationCreationFailPayload("재고 부족")
                OutboundEventType.RESERVATION_CONFIRM -> ReservationConfirmSuccessPayload(
                    UUID.randomUUID(),
                    listOf(ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 10L))
                )

                OutboundEventType.RESERVATION_RELEASE -> ReservationReleaseSuccessPayload(
                    UUID.randomUUID(),
                    listOf(ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 10L))
                )
            }

            val event = ReservationOutboundEvent(
                eventId = UUID.randomUUID(),
                outboxId = UUID.randomUUID(),
                orderId = UUID.randomUUID(),
                eventType = eventType,
                payload = payload
            )

            val result = producer.produce(event).get(10, TimeUnit.SECONDS)
            assertTrue(result is ProduceResult.Success)
            assertEquals(event.eventId, (result as ProduceResult.Success).eventId)
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
                eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
                payload = ReservationCreationSuccessPayload(
                    UUID.randomUUID(),
                    listOf(ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 10L))
                )
            )
        }

        // when - 비동기 프로듀스
        val futures = events.map { producer.produce(it) }

        // then - 모든 결과 확인
        futures.forEach { future ->
            val result = future.get(10, TimeUnit.SECONDS)
            assertTrue(result is ProduceResult.Success)
        }
    }

    @Test
    fun `각 이벤트가 고유한 eventId와 outboxId를 가진다`() {
        // given
        val events = (1..10).map {
            ReservationOutboundEvent(
                eventId = UUID.randomUUID(),
                outboxId = UUID.randomUUID(),
                orderId = UUID.randomUUID(),
                eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
                payload = ReservationCreationSuccessPayload(
                    UUID.randomUUID(),
                    listOf(ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 10L))
                )
            )
        }

        // when
        val results = events.map { producer.produce(it).get(10, TimeUnit.SECONDS) }

        // then
        assertTrue(results.all { it is ProduceResult.Success })
        val eventIds = events.map { it.eventId }
        val outboxIds = events.map { it.outboxId }
        assertEquals(eventIds.size, eventIds.distinct().size)
        assertEquals(outboxIds.size, outboxIds.distinct().size)
    }
}