package nuts.commerce.inventoryservice.event.inbound.handler

import nuts.commerce.inventoryservice.event.inbound.InboundEventType
import nuts.commerce.inventoryservice.event.inbound.ReservationInboundEvent

interface ReservationEventHandler {
    val supportType: InboundEventType
    fun handle(reservationInboundEvent: ReservationInboundEvent)
}