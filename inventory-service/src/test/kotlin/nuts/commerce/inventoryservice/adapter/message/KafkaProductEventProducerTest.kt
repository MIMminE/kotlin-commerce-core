package nuts.commerce.inventoryservice.adapter.message

import nuts.commerce.inventoryservice.event.outbound.ProductEventType
import nuts.commerce.inventoryservice.event.outbound.ProductOutboundEvent
import nuts.commerce.inventoryservice.event.outbound.ProductStockDecrementPayload
import nuts.commerce.inventoryservice.event.outbound.ProductStockIncrementPayload
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
    classes = [KafkaProductEventProducer::class]
)
@ImportAutoConfiguration(KafkaAutoConfiguration::class)
class KafkaProductEventProducerTest {

    @Autowired
    lateinit var producer: KafkaProductEventProducer

    companion object {
        @Container
        @ServiceConnection
        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"))
    }

    @Test
    fun `이벤트를 성공적으로 프로듀스할 수 있다`() {
        // given
        val eventId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val quantity = 10L

        val event = ProductOutboundEvent(
            eventId = eventId,
            eventType = ProductEventType.DECREMENT_STOCK,
            payload = ProductStockDecrementPayload(
                orderId = orderId,
                productId = productId,
                qty = quantity
            )
        )

        // when
        val resultFuture = producer.produce(event)
        val result = resultFuture.get(10, TimeUnit.SECONDS)

        // then
        assertNotNull(result)
        assertTrue(result)
    }

    @Test
    fun `서로 다른 이벤트 타입을 프로듀스할 수 있다`() {
        // given
        val eventTypes = listOf(
            ProductEventType.INCREMENT_STOCK,
            ProductEventType.DECREMENT_STOCK
        )

        // when & then
        eventTypes.forEach { eventType ->
            val event = ProductOutboundEvent(
                eventId = UUID.randomUUID(),
                eventType = eventType,
                payload = when (eventType) {
                    ProductEventType.INCREMENT_STOCK -> ProductStockIncrementPayload(
                        orderId = UUID.randomUUID(),
                        productId = UUID.randomUUID(),
                        qty = 5L
                    )

                    ProductEventType.DECREMENT_STOCK -> ProductStockDecrementPayload(
                        orderId = UUID.randomUUID(),
                        productId = UUID.randomUUID(),
                        qty = 10L
                    )

                    else -> throw IllegalArgumentException("Unsupported event type")
                }
            )

            val result = producer.produce(event)
                .get(10, TimeUnit.SECONDS)

            assertTrue(result)
        }
    }

    @Test
    fun `비동기 프로듀스 결과를 올바르게 처리한다`() {
        // given
        val events = (1..10).map {
            ProductOutboundEvent(
                eventId = UUID.randomUUID(),
                eventType = ProductEventType.DECREMENT_STOCK,
                payload = ProductStockDecrementPayload(
                    orderId = UUID.randomUUID(),
                    productId = UUID.randomUUID(),
                    qty = 10L
                )
            )
        }

        // when - 비동기 프로듀스
        val futures = events.map { producer.produce(it) }

        // then - 모든 결과 확인
        futures.forEach { future ->
            val result = future.get(10, TimeUnit.SECONDS)
            assertTrue(result)
        }
    }

    @Test
    fun `재고 감소 이벤트를 올바른 페이로드와 함께 프로듀스할 수 있다`() {
        // given
        val eventId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val decrementQuantity = 50L

        val event = ProductOutboundEvent(
            eventId = eventId,
            eventType = ProductEventType.DECREMENT_STOCK,
            payload = ProductStockDecrementPayload(
                orderId = orderId,
                productId = productId,
                qty = decrementQuantity
            )
        )

        // when
        val result = producer.produce(event)
            .get(10, TimeUnit.SECONDS)

        // then
        assertTrue(result)
        val payload = event.payload as ProductStockDecrementPayload
        assertEquals(orderId, payload.orderId)
        assertEquals(productId, payload.productId)
        assertEquals(decrementQuantity, payload.qty)
    }

    @Test
    fun `재고 증가 이벤트를 올바른 페이로드와 함께 프로듀스할 수 있다`() {
        // given
        val eventId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val incrementQuantity = 30L

        val event = ProductOutboundEvent(
            eventId = eventId,
            eventType = ProductEventType.INCREMENT_STOCK,
            payload = ProductStockIncrementPayload(
                orderId = orderId,
                productId = productId,
                qty = incrementQuantity
            )
        )

        // when
        val result = producer.produce(event)
            .get(10, TimeUnit.SECONDS)

        // then
        assertTrue(result)
        val payload = event.payload as ProductStockIncrementPayload
        assertEquals(orderId, payload.orderId)
        assertEquals(productId, payload.productId)
        assertEquals(incrementQuantity, payload.qty)
    }

    @Test
    fun `대량의 재고 감소 이벤트를 병렬로 프로듀스할 수 있다`() {
        // given
        val events = (1..20).map {
            ProductOutboundEvent(
                eventId = UUID.randomUUID(),
                eventType = ProductEventType.DECREMENT_STOCK,
                payload = ProductStockDecrementPayload(
                    orderId = UUID.randomUUID(),
                    productId = UUID.randomUUID(),
                    qty = (it * 5).toLong()
                )
            )
        }

        // when - 병렬 프로듀스
        val futures = events.map { producer.produce(it) }

        // then - 모든 결과가 성공적임을 확인
        futures.forEach { future ->
            val result = future.get(10, TimeUnit.SECONDS)
            assertTrue(result)
        }
        assertEquals(20, futures.size)
    }

    @Test
    fun `서로 다른 주문의 이벤트를 함께 프로듀스할 수 있다`() {
        // given
        val orderIds = (1..3).map { UUID.randomUUID() }
        val events = orderIds.flatMap { orderId ->
            (1..3).map { productIndex ->
                ProductOutboundEvent(
                    eventId = UUID.randomUUID(),
                    eventType = ProductEventType.DECREMENT_STOCK,
                    payload = ProductStockDecrementPayload(
                        orderId = orderId,
                        productId = UUID.randomUUID(),
                        qty = (productIndex * 10).toLong()
                    )
                )
            }
        }

        // when
        val results = events.map { event ->
            producer.produce(event)
                .get(10, TimeUnit.SECONDS)
        }

        // then
        assertEquals(9, results.size)  // 3 orders * 3 products
        results.forEach { assertTrue(it) }
    }


    @Test
    fun `각 이벤트가 고유한 eventId를 가진다`() {
        // given
        val events = (1..10).map {
            ProductOutboundEvent(
                eventId = UUID.randomUUID(),  // 각각 고유한 UUID
                eventType = ProductEventType.DECREMENT_STOCK,
                payload = ProductStockDecrementPayload(
                    orderId = UUID.randomUUID(),
                    productId = UUID.randomUUID(),
                    qty = 10L
                )
            )
        }

        // when
        val results = events.map { producer.produce(it).get(10, TimeUnit.SECONDS) }

        // then
        assertTrue(results.all { it })
        val eventIds = events.map { it.eventId }
        assertEquals(eventIds.size, eventIds.distinct().size)  // 모든 eventId가 고유함
    }
}