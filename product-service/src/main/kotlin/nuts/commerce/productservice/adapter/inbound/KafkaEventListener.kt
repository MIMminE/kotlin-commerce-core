package nuts.commerce.productservice.adapter.inbound

import jakarta.annotation.PostConstruct
import nuts.commerce.productservice.adapter.inbound.ListenEventType.*
import nuts.commerce.productservice.usecase.UpdateStockCommand
import nuts.commerce.productservice.usecase.StockUpdateUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@ConditionalOnProperty(
    prefix = "product.kafka.listener",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class KafkaEventListener(
    private val productUpdateUseCase: StockUpdateUseCase,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        logger.info("KafkaEventListener initialized")
    }

    @KafkaListener(topics = [$$"${product.kafka.listener.topic}"])
    fun onMessage(
        @Payload envelope: KafkaEventEnvelope,
        @Header(KafkaHeaders.RECEIVED_KEY) key: String
    ) {
        when (envelope.eventType) {
            RESERVATION_CREATION_SUCCEEDED -> handleUpdateStock(envelope)
            RESERVATION_RELEASE -> handleUpdateStock(envelope)
        }
    }

    private fun handleUpdateStock(envelope: KafkaEventEnvelope) {
        val payload = objectMapper.treeToValue(envelope.payload, UpdateStockPayload::class.java)
        val command = UpdateStockCommand(payload.updateStockItems.map {
            UpdateStockCommand.StockUpdateItem(
                productId = it.productId,
                expectQuantity = it.expectQuantity,
                updateQuantity = it.updateQuantity
            )
        })
        productUpdateUseCase.execute(command)
    }
}