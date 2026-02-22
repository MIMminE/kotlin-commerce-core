package nuts.commerce.inventoryservice.event.outbound.converter

import nuts.commerce.inventoryservice.event.outbound.OutboundEventType
import nuts.commerce.inventoryservice.event.outbound.ProductEventType
import nuts.commerce.inventoryservice.event.outbound.ProductOutboundEvent
import nuts.commerce.inventoryservice.event.outbound.ProductStockDecrementPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationReleaseSuccessPayload
import nuts.commerce.inventoryservice.model.OutboxInfo
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ProductStockDecrementEventConverter(private val objectMapper: ObjectMapper) :
    OutboundEventConverter<ProductOutboundEvents, OutboundEventType> {
    override val supportType: OutboundEventType
        get() = OutboundEventType.RESERVATION_RELEASE

    override fun convert(outboxInfo: OutboxInfo): ProductOutboundEvents {
        return ProductOutboundEvents(
            objectMapper.readValue(outboxInfo.payload, ReservationReleaseSuccessPayload::class.java)
                .reservationItemInfoList
                .map { reservationItem ->
                    ProductOutboundEvent(
                        eventType = ProductEventType.INCREMENT_STOCK,
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