package nuts.commerce.productservice.adapter.system

import nuts.commerce.productservice.event.inbound.InboundEventType
import nuts.commerce.productservice.event.inbound.ProductCreatedPayload
import nuts.commerce.productservice.event.inbound.ProductInboundEvent
import nuts.commerce.productservice.event.inbound.handler.ProductEventHandler
import nuts.commerce.productservice.event.inbound.ProductStockDecrementPayload
import nuts.commerce.productservice.event.inbound.ProductStockIncrementPayload
import nuts.commerce.productservice.event.inbound.handler.ProductCreateHandler
import nuts.commerce.productservice.event.inbound.handler.ProductStockDecrementHandler
import nuts.commerce.productservice.event.inbound.handler.ProductStockIncrementHandler
import org.junit.jupiter.api.BeforeEach
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
import kotlin.test.Test

@Suppress("NonAsciiCharacters")
@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(
    classes = [ProductEventListener::class, TestHandlerConfig::class]
)
@ImportAutoConfiguration(KafkaAutoConfiguration::class)
class ProductEventListenerTest {

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, ProductInboundEvent>

    @Autowired
    @field:Qualifier("createdHandler")
    lateinit var createdHandler: ProductEventHandler

    @Autowired
    @field:Qualifier("incrementHandler")
    lateinit var incrementHandler: ProductEventHandler

    @Autowired
    @field:Qualifier("decrementHandler")
    lateinit var decrementHandler: ProductEventHandler

    companion object {
        @Container
        @ServiceConnection
        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"))
    }

    @BeforeEach
    fun resetMocks() {
        org.mockito.Mockito.reset(createdHandler, incrementHandler, decrementHandler)
        whenever(createdHandler.supportType).thenReturn(InboundEventType.CREATED)
        whenever(incrementHandler.supportType).thenReturn(InboundEventType.INCREMENT_STOCK)
        whenever(decrementHandler.supportType).thenReturn(InboundEventType.DECREMENT_STOCK)
    }

    @Test
    fun `CREATED 이벤트는 createdHandler로 전달된다`() {
        val event = ProductInboundEvent(
            eventId = UUID.randomUUID(),
            eventType = InboundEventType.CREATED,
            payload = ProductCreatedPayload(UUID.randomUUID(), 10L)
        )

        kafkaTemplate.send("product-event", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        verify(createdHandler, timeout(10_000)).handle(any())
        verify(incrementHandler, never()).handle(any())
        verify(decrementHandler, never()).handle(any())
    }

    @Test
    fun `INCREMENT_STOCK 이벤트는 incrementHandler로 전달된다`() {
        val event = ProductInboundEvent(
            eventId = UUID.randomUUID(),
            eventType = InboundEventType.INCREMENT_STOCK,
            payload = ProductStockIncrementPayload(UUID.randomUUID(), UUID.randomUUID(), 3L)
        )

        kafkaTemplate.send("product-event", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        verify(incrementHandler, timeout(10_000)).handle(any())
        verify(createdHandler, never()).handle(any())
        verify(decrementHandler, never()).handle(any())
    }

    @Test
    fun `DECREMENT_STOCK 이벤트는 decrementHandler로 전달된다`() {
        val event = ProductInboundEvent(
            eventId = UUID.randomUUID(),
            eventType = InboundEventType.DECREMENT_STOCK,
            payload = ProductStockDecrementPayload(UUID.randomUUID(), UUID.randomUUID(), 2L)
        )

        kafkaTemplate.send("product-event", event).get(5, TimeUnit.SECONDS)
        kafkaTemplate.flush()

        verify(decrementHandler, timeout(10_000)).handle(any())
        verify(createdHandler, never()).handle(any())
        verify(incrementHandler, never()).handle(any())
    }
}

@TestConfiguration(proxyBeanMethods = false)
class TestHandlerConfig {

    @Bean("createdHandler")
    fun createdHandler(): ProductCreateHandler {
        val handler = mock<ProductCreateHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.CREATED)
        return handler
    }

    @Bean("incrementHandler")
    fun incrementHandler(): ProductStockIncrementHandler {
        val handler = mock<ProductStockIncrementHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.INCREMENT_STOCK)
        return handler
    }

    @Bean("decrementHandler")
    fun decrementHandler(): ProductStockDecrementHandler {
        val handler = mock<ProductStockDecrementHandler>()
        whenever(handler.supportType).thenReturn(InboundEventType.DECREMENT_STOCK)
        return handler
    }
}