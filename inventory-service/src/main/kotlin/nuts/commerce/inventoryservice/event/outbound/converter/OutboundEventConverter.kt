package nuts.commerce.inventoryservice.event.outbound.converter

import nuts.commerce.inventoryservice.model.OutboxInfo

interface OutboundEventConverter<E, EventType : Enum<EventType>> {
    val supportType: EventType
    fun convert(outboxInfo: OutboxInfo): E
}