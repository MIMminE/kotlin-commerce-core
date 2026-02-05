package nuts.commerce.paymentservice.application.port.message

import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryPaymentEventProducer(
    private val wiredConsumer: PaymentEventConsumer? = null
) : PaymentEventProducer {

    private val _produced = CopyOnWriteArrayList<PaymentEventProducer.Message>()
    val produced: List<PaymentEventProducer.Message> get() = _produced

    override fun produce(message: PaymentEventProducer.Message) {
        _produced += message
        wiredConsumer?.consume(
            PaymentEventConsumer.InboundMessage(
                eventId = message.eventId,
                eventType = when (message) {
                    is PaymentEventProducer.Message.PaymentApproved -> "PaymentApproved"
                    is PaymentEventProducer.Message.PaymentDeclined -> "PaymentDeclined"
                    else -> "Unknown"
                },
                payload = "{\"eventId\":\"${message.eventId}\"}",
                aggregateId = message.orderId
            )
        )
    }

    fun clear() = _produced.clear()
}

