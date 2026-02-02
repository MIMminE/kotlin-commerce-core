package nuts.commerce.orderservice.application.port.message

import java.util.UUID

interface MessagePublisher {
    fun publish(eventId: UUID,
                eventType: String,
                payload: String,
                aggregateId: UUID)
}