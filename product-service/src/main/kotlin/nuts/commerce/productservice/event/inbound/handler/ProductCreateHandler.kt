package nuts.commerce.productservice.event.inbound.handler

import nuts.commerce.productservice.event.inbound.InboundEventType
import nuts.commerce.productservice.event.inbound.ProductCreatedPayload
import nuts.commerce.productservice.event.inbound.ProductInboundEvent
import nuts.commerce.productservice.model.ProductEventInbox
import nuts.commerce.productservice.port.cache.StockCachePort
import nuts.commerce.productservice.port.repository.ProductEventInboxRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper

@Component
class ProductCreateHandler(
    private val stockCachePort: StockCachePort,
    private val productEventInboxRepository: ProductEventInboxRepository,
    private val txTemplate: TransactionTemplate,
    private val objectMapper: ObjectMapper
) : ProductEventHandler {

    override val supportType: InboundEventType
        get() = InboundEventType.CREATED

    override fun handle(productInboundEvent: ProductInboundEvent) {
        require(productInboundEvent.eventType == InboundEventType.CREATED)

        val payload = productInboundEvent.payload as ProductCreatedPayload

        val inboxRecord = ProductEventInbox.create(
            idempotencyKey = productInboundEvent.eventId,
            payload = objectMapper.writeValueAsString(payload)
        )
        txTemplate.execute {
            productEventInboxRepository.save(inboxRecord)
        }
        stockCachePort.saveStock(
            productId = payload.productId,
            stock = payload.stock
        )
    }
}