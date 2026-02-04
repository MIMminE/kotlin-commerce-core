package nuts.commerce.paymentservice.application.port.message

import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryPaymentEventConsumer : PaymentEventConsumer {

    private val _consumed = CopyOnWriteArrayList<PaymentEventConsumer.InboundMessage>()
    val consumed: List<PaymentEventConsumer.InboundMessage> get() = _consumed

    override fun consume(message: PaymentEventConsumer.InboundMessage) {
        _consumed += message
    }

    fun clear() = _consumed.clear()

    fun lastOrNull(): PaymentEventConsumer.InboundMessage? = _consumed.lastOrNull()
}

