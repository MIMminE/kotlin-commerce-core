package nuts.commerce.paymentservice.adapter.message

import nuts.commerce.paymentservice.event.outbound.OutboundEventType
import nuts.commerce.paymentservice.event.outbound.PaymentConfirmSuccessPayload
import nuts.commerce.paymentservice.event.outbound.PaymentOutboundEvent
import nuts.commerce.paymentservice.port.message.ProduceResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.kafka.core.KafkaTemplate
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
    fun `이벤트를 성공적으로 프로듀스할 수 있다`() {
        // given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val providerPaymentId = UUID.randomUUID()

        val event = PaymentOutboundEvent(
            eventId = eventId,
            outboxId = outboxId,
            orderId = orderId,
            paymentId = paymentId,
            eventType = OutboundEventType.PAYMENT_CONFIRM,
            payload = PaymentConfirmSuccessPayload(
                paymentProvider = "TOSS",
                providerPaymentId = providerPaymentId
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
    fun `여러 이벤트를 연속으로 프로듀스할 수 있다`() {
        // given
        val events = (1..5).map { i ->
            PaymentOutboundEvent(
                eventId = UUID.randomUUID(),
                outboxId = UUID.randomUUID(),
                orderId = UUID.randomUUID(),
                paymentId = UUID.randomUUID(),
                eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
                payload = PaymentConfirmSuccessPayload(
                    paymentProvider = "TOSS",
                    providerPaymentId = UUID.randomUUID()
                )
            )
        }

        // when
        val results = events.map { event ->
            producer.produce(event)
                .get(10, TimeUnit.SECONDS)
        }

        // then
        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result is ProduceResult.Success)
        }
    }

    @Test
    fun `서로 다른 이벤트 타입을 프로듀스할 수 있다`() {
        // given
        val eventTypes = listOf(
            OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
            OutboundEventType.PAYMENT_CREATION_FAILED,
            OutboundEventType.PAYMENT_CONFIRM,
            OutboundEventType.PAYMENT_RELEASE
        )

        // when & then
        eventTypes.forEach { eventType ->
            val event = PaymentOutboundEvent(
                eventId = UUID.randomUUID(),
                outboxId = UUID.randomUUID(),
                orderId = UUID.randomUUID(),
                paymentId = UUID.randomUUID(),
                eventType = eventType,
                payload = PaymentConfirmSuccessPayload(
                    paymentProvider = "TOSS",
                    providerPaymentId = UUID.randomUUID()
                )
            )

            val result = producer.produce(event)
                .get(10, TimeUnit.SECONDS)

            assertTrue(result is ProduceResult.Success)
            assertEquals(event.eventId, (result as ProduceResult.Success).eventId)
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
                paymentId = UUID.randomUUID(),
                eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
                payload = PaymentConfirmSuccessPayload(
                    paymentProvider = "TOSS",
                    providerPaymentId = UUID.randomUUID()
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
}