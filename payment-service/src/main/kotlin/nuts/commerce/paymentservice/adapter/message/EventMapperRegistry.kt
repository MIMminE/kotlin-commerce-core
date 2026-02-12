package nuts.commerce.paymentservice.adapter.message

import nuts.commerce.paymentservice.model.EventType
import nuts.commerce.paymentservice.model.OutboxInfo
import nuts.commerce.paymentservice.port.message.PaymentEvent
import nuts.commerce.paymentservice.port.message.PaymentEventMapper
import org.springframework.stereotype.Component

@Component
class EventMapperRegistry(mappers: List<PaymentEventMapper>) {
    private val registry: Map<EventType, PaymentEventMapper> = run {
        val grouped = mappers.groupBy { it.eventType }
        val duplicates = grouped.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) {
            "Duplicate PaymentEventMapper for eventType(s): $duplicates"
        }
        grouped.mapValues { (_, v) -> v.single() }
    }

    fun map(outboxInfo: OutboxInfo): PaymentEvent {
        val mapper = registry[outboxInfo.eventType]
            ?: error("No PaymentEventMapper for eventType=${outboxInfo.eventType}")
        return mapper.map(outboxInfo)
    }
}