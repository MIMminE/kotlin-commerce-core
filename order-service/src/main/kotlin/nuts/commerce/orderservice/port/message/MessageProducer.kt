package nuts.commerce.orderservice.port.message

import java.util.UUID

interface MessageProducer {
    fun produce(produceMessage: ProduceMessage)

    data class ProduceMessage(
        val eventId: UUID,
        val eventType: String,
        val payload: String,
        val aggregateId: UUID
    )
}