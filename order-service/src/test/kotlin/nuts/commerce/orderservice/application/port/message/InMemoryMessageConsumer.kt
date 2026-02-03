package nuts.commerce.orderservice.application.port.message

import java.util.concurrent.CopyOnWriteArrayList

class InMemoryMessageConsumer : MessageConsumer {

    private val _paymentResults = CopyOnWriteArrayList<MessageConsumer.PaymentResultMessage>()
    val paymentResults: List<MessageConsumer.PaymentResultMessage> get() = _paymentResults

    override fun onPaymentResult(message: MessageConsumer.PaymentResultMessage) {
        _paymentResults += message
    }

    fun clear() = _paymentResults.clear()

    fun lastOrNull(): MessageConsumer.PaymentResultMessage? = _paymentResults.lastOrNull()
}