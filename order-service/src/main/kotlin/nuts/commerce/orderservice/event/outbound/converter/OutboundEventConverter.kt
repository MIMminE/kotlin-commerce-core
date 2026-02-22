package nuts.commerce.orderservice.event.outbound.converter

import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.model.OutboxInfo

interface OutboundEventConverter<E> {
    val supportType: OutboundEventType
    fun convert(outboxInfo: OutboxInfo): E
}