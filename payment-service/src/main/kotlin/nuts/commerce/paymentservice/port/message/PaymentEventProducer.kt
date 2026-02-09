package nuts.commerce.paymentservice.port.message

import nuts.commerce.paymentservice.event.OutgoingEvent


interface PaymentEventProducer {
    fun produce(outgoingEvent: OutgoingEvent)
}