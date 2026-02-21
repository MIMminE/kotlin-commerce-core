package nuts.commerce.productservice.event.handler

import nuts.commerce.productservice.event.InboundEventType
import nuts.commerce.productservice.event.ProductInboundEvent
import nuts.commerce.productservice.event.ProductStockDecrementPayload
import nuts.commerce.productservice.model.ProductEventInbox
import nuts.commerce.productservice.port.cache.StockCachePort
import nuts.commerce.productservice.port.repository.ProductEventInboxRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper

@Component
class ProductStockDecrementHandler(
    private val stockCachePort: StockCachePort,
    private val productEventInboxRepository: ProductEventInboxRepository,
    private val txTemplate: TransactionTemplate,
    private val objectMapper: ObjectMapper
) : ProductEventHandler {

    override val supportType: InboundEventType
        get() = InboundEventType.DECREMENT_STOCK

    override fun handle(productInboundEvent: ProductInboundEvent) {
        require(productInboundEvent.eventType == InboundEventType.DECREMENT_STOCK)

        val payload = productInboundEvent.payload as ProductStockDecrementPayload

        val inboxRecord = ProductEventInbox.create(
            idempotencyKey = productInboundEvent.eventId,
            payload = objectMapper.writeValueAsString(payload)
        )

        txTemplate.execute {
            productEventInboxRepository.save(inboxRecord)
        }

        stockCachePort.minusStock(
            productId = payload.productId,
            minusStock = payload.qty
        )
    }
}