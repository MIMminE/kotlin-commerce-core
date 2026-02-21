package nuts.commerce.productservice.event.inbound.handler

import nuts.commerce.productservice.event.inbound.InboundEventType
import nuts.commerce.productservice.event.inbound.ProductInboundEvent
import nuts.commerce.productservice.event.inbound.ProductStockIncrementPayload
import nuts.commerce.productservice.model.ProductEventInbox
import nuts.commerce.productservice.port.cache.StockCachePort
import nuts.commerce.productservice.port.repository.ProductEventInboxRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper

@Component
class ProductStockIncrementHandler(
    private val stockCachePort: StockCachePort,
    private val productEventInboxRepository: ProductEventInboxRepository,
    private val txTemplate: TransactionTemplate,
    private val objectMapper: ObjectMapper
) : ProductEventHandler {

    override val supportType: InboundEventType
        get() = InboundEventType.INCREMENT_STOCK

    override fun handle(productInboundEvent: ProductInboundEvent) {
        require(productInboundEvent.eventType == InboundEventType.INCREMENT_STOCK)

        val payload = productInboundEvent.payload as ProductStockIncrementPayload

        val inboxRecord = ProductEventInbox.create(
            idempotencyKey = productInboundEvent.eventId,
            payload = objectMapper.writeValueAsString(payload)
        )

        txTemplate.execute {
            productEventInboxRepository.save(inboxRecord)
        }

        stockCachePort.plusStock(
            productId = payload.productId,
            plusStock = payload.qty
        )
    }
}