package nuts.commerce.productservice.usecase

import nuts.commerce.productservice.event.InboundEventType
import nuts.commerce.productservice.event.ProductInboundEvent
import nuts.commerce.productservice.event.ReservationConfirmSuccessPayload
import nuts.commerce.productservice.model.StockUpdateInboxRecord
import nuts.commerce.productservice.port.cache.StockCachePort
import nuts.commerce.productservice.port.repository.StockUpdateInboxRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper

@Component
class ReservationReleaseHandler(
    private val stockCachePort: StockCachePort,
    private val stockUpdateInboxRepository: StockUpdateInboxRepository,
    private val txTemplate: TransactionTemplate,
    private val objectMapper: ObjectMapper
) {

    fun handle(productInboundEvent: ProductInboundEvent) {
        require(productInboundEvent.eventType == InboundEventType.RESERVATION_RELEASE)
        require(productInboundEvent.payload is ReservationConfirmSuccessPayload)

        val payload = productInboundEvent.payload

        val inboxRecord = StockUpdateInboxRecord.create(
            orderId = productInboundEvent.orderId,
            reservationId = productInboundEvent.reservationId,
            idempotencyKey = productInboundEvent.eventId,
            payload = objectMapper.writeValueAsString(payload)
        )

        txTemplate.execute {
            stockUpdateInboxRepository.save(inboxRecord)
        }

        payload.reservationItemInfoList.forEach {
            stockCachePort.updateStock(it.productId, -it.qty)
        }
    }
}