package nuts.commerce.orderservice.adapter.message

import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.PaymentConfirmPayload
import nuts.commerce.orderservice.event.outbound.PaymentCreateFailedPayload
import nuts.commerce.orderservice.event.outbound.PaymentCreatePayload
import nuts.commerce.orderservice.event.outbound.PaymentOutboundEvent
import nuts.commerce.orderservice.event.outbound.PaymentReleasePayload
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
    classes = [KafkaPaymentEventProducer::class]
)
@ImportAutoConfiguration(KafkaAutoConfiguration::class)
class KafkaPaymentEventProducerTest {

    @Autowired
    lateinit var producer: KafkaPaymentEventProducer

    companion object {
        @Container
        @ServiceConnection
        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"))
    }

    @Test
    fun `결제 이벤트를 성공적으로 프로듀스할 수 있다`() {
        // given
        val eventId = UUID.randomUUID()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()

        val event = PaymentOutboundEvent(
            eventId = eventId,
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.PAYMENT_CREATE_REQUEST,
            payload = PaymentCreatePayload(
                amount = 10000,
                currency = "KRW"
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
    fun `서로 다른 결제 이벤트 타입을 프로듀스할 수 있다`() {
        // given
        val eventTypes = listOf(
            OutboundEventType.PAYMENT_CREATE_REQUEST,
            OutboundEventType.PAYMENT_CREATE_FAILED,
            OutboundEventType.PAYMENT_CONFIRM_REQUEST,
            OutboundEventType.PAYMENT_RELEASE_REQUEST
        )

        // when & then
        eventTypes.forEach { eventType ->
            val event = PaymentOutboundEvent(
                eventId = UUID.randomUUID(),
                outboxId = UUID.randomUUID(),
                orderId = UUID.randomUUID(),
                eventType = eventType,
                payload = when (eventType) {
                    OutboundEventType.PAYMENT_CREATE_REQUEST -> PaymentCreatePayload(
                        amount = 10000,
                        currency = "KRW"
                    )

                    OutboundEventType.PAYMENT_CREATE_FAILED -> PaymentCreateFailedPayload(
                        reason = "insufficient balance"
                    )

                    OutboundEventType.PAYMENT_CONFIRM_REQUEST -> PaymentConfirmPayload(
                        paymentId = UUID.randomUUID()
                    )

                    OutboundEventType.PAYMENT_RELEASE_REQUEST -> PaymentReleasePayload(
                        paymentId = UUID.randomUUID()
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
            PaymentOutboundEvent(
                eventId = UUID.randomUUID(),
                outboxId = UUID.randomUUID(),
                orderId = UUID.randomUUID(),
                eventType = OutboundEventType.PAYMENT_CREATE_REQUEST,
                payload = PaymentCreatePayload(
                    amount = (it * 1000).toLong(),
                    currency = "KRW"
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
    fun `결제 생성 요청 이벤트를 올바른 페이로드와 함께 프로듀스할 수 있다`() {
        // given
        val eventId = UUID.randomUUID()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val amount = 50000L
        val currency = "KRW"

        val event = PaymentOutboundEvent(
            eventId = eventId,
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.PAYMENT_CREATE_REQUEST,
            payload = PaymentCreatePayload(
                amount = amount,
                currency = currency
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

        val payload = event.payload as PaymentCreatePayload
        assertEquals(amount, payload.amount)
        assertEquals(currency, payload.currency)
    }

    @Test
    fun `결제 실패 이벤트를 올바른 페이로드와 함께 프로듀스할 수 있다`() {
        // given
        val eventId = UUID.randomUUID()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reason = "카드 거절됨"

        val event = PaymentOutboundEvent(
            eventId = eventId,
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.PAYMENT_CREATE_FAILED,
            payload = PaymentCreateFailedPayload(
                reason = reason
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

        val payload = event.payload as PaymentCreateFailedPayload
        assertEquals(reason, payload.reason)
    }

    @Test
    fun `결제 확정 이벤트를 올바른 페이로드와 함께 프로듀스할 수 있다`() {
        // given
        val eventId = UUID.randomUUID()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()

        val event = PaymentOutboundEvent(
            eventId = eventId,
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.PAYMENT_CONFIRM_REQUEST,
            payload = PaymentConfirmPayload(
                paymentId = paymentId
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

        val payload = event.payload as PaymentConfirmPayload
        assertEquals(paymentId, payload.paymentId)
    }

    @Test
    fun `결제 해제 이벤트를 올바른 페이로드와 함께 프로듀스할 수 있다`() {
        // given
        val eventId = UUID.randomUUID()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()

        val event = PaymentOutboundEvent(
            eventId = eventId,
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.PAYMENT_RELEASE_REQUEST,
            payload = PaymentReleasePayload(
                paymentId = paymentId
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

        val payload = event.payload as PaymentReleasePayload
        assertEquals(paymentId, payload.paymentId)
    }

    @Test
    fun `각 이벤트가 고유한 eventId를 가진다`() {
        // given
        val events = (1..10).map {
            PaymentOutboundEvent(
                eventId = UUID.randomUUID(),  // 각각 고유한 UUID
                outboxId = UUID.randomUUID(),
                orderId = UUID.randomUUID(),
                eventType = OutboundEventType.PAYMENT_CREATE_REQUEST,
                payload = PaymentCreatePayload(
                    amount = 10000,
                    currency = "KRW"
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
            PaymentOutboundEvent(
                eventId = UUID.randomUUID(),
                outboxId = UUID.randomUUID(),  // 각각 고유한 UUID
                orderId = UUID.randomUUID(),
                eventType = OutboundEventType.PAYMENT_CREATE_REQUEST,
                payload = PaymentCreatePayload(
                    amount = 10000,
                    currency = "KRW"
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
