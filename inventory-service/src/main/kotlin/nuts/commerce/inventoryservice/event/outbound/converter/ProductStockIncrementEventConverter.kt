package nuts.commerce.inventoryservice.event.outbound.converter

import nuts.commerce.inventoryservice.event.outbound.OutboundEventType
import nuts.commerce.inventoryservice.event.outbound.ProductEventType
import nuts.commerce.inventoryservice.event.outbound.ProductOutboundEvent
import nuts.commerce.inventoryservice.event.outbound.ProductStockDecrementPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationCreationSuccessPayload
import nuts.commerce.inventoryservice.model.OutboxInfo
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ProductStockIncrementEventConverter(private val objectMapper: ObjectMapper) :
    OutboundEventConverter<ProductOutboundEvents, OutboundEventType> {
    override val supportType: OutboundEventType
        get() = OutboundEventType.RESERVATION_CREATION_SUCCEEDED

    override fun convert(outboxInfo: OutboxInfo): ProductOutboundEvents {
        return ProductOutboundEvents(
            objectMapper.readValue(outboxInfo.payload, ReservationCreationSuccessPayload::class.java)
                .reservationItemInfoList
                .map { reservationItem ->
                    ProductOutboundEvent(
                        eventType = ProductEventType.DECREMENT_STOCK,
                        payload = ProductStockDecrementPayload(
                            orderId = outboxInfo.orderId,
                            productId = reservationItem.productId,
                            qty = reservationItem.qty
                        )
                    )
                }
        )
    }
}