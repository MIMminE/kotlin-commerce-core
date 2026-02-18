package nuts.commerce.productservice.usecase

import nuts.commerce.productservice.adapter.repository.StockUpdateInboxJpa
import nuts.commerce.productservice.event.InboundEventType
import nuts.commerce.productservice.event.ProductInboundEvent
import nuts.commerce.productservice.event.ReservationCreationSuccessPayload
import nuts.commerce.productservice.model.StockUpdateInboxRecord
import nuts.commerce.productservice.port.cache.StockCachePort
import org.springframework.stereotype.Component
import org.springframework.transaction.TransactionManager
import tools.jackson.databind.ObjectMapper

@Component
class ReservationCreationSucceededHandler(
    private val stockCachePort: StockCachePort,
    private val stockUpdateInboxJpa: StockUpdateInboxJpa,
    private val txManager: TransactionManager,
    private val objectMapper: ObjectMapper
) {

    fun handle(productInboundEvent: ProductInboundEvent){
        require(productInboundEvent.eventType == InboundEventType.RESERVATION_CREATION_SUCCEEDED)
        require(productInboundEvent.payload is ReservationCreationSuccessPayload)

        val payload = productInboundEvent.payload
        val reservationItemInfoList = payload.reservationItemInfoList

        val inboxRecord = StockUpdateInboxRecord.create(
            orderId = productInboundEvent.orderId,
            reservationId = productInboundEvent.reservationId,
            idempotencyKey = productInboundEvent.eventId,
            payload = objectMapper.writeValueAsString(payload)
        )

        stockUpdateInboxJpa.save(inboxRecord)



        reservationItemInfoList.forEach {
            stockCachePort.saveStock(it.productId, it.qty, -it.qty)
        }

    }
}