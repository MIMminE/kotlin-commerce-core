package nuts.commerce.paymentservice.port.message

import nuts.commerce.paymentservice.model.event.OutgoingEvent


interface PaymentEventProducer {
    fun produce(outgoingEvent: OutgoingEvent)
}